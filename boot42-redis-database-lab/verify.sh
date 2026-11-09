#!/usr/bin/env bash
# Reproduces every row of the behaviour table in the companion article.
#
# No Redis server is contacted. Lettuce creates its client lazily, so the
# application context starts and closes without a network connection. Each case
# is one fresh JVM and one set of command-line properties. The probe reports two
# numbers: the database written in configuration, and the database the
# auto-configured connection factory was actually built with.
#
# The build always runs clean. A stale application.properties under
# target/<version>/classes silently overrides the command line and makes every
# row report the same configuration, which is how the first run of this lab
# produced a uniformly wrong table.
set -u
cd "$(dirname "$0")" || exit 2

need() { command -v "$1" >/dev/null 2>&1 || { echo "missing: $1"; exit 2; }; }
need java
need mvn
need python3
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi

OUT=verification-output
EVID=evidence
rm -rf "$OUT"
mkdir -p "$OUT/evidence" "$EVID"
rm -f "$EVID"/*.log

PASS=0
FAIL=0

echo "== build (two pinned parents) =="
mvn -f pom-412.xml -B -q clean package -DskipTests || { echo "build 4.1.2 failed"; exit 2; }
mvn -f pom-rc2.xml -B -q clean package -DskipTests || { echo "build 4.2.0-RC2 failed"; exit 2; }

J412=target/412/probe-412.jar
JRC2=target/rc2/probe-rc2.jar
NODES=(--spring.data.redis.masterreplica.nodes=127.0.0.1:6379,127.0.0.1:6380)
STAND=(--spring.data.redis.host=127.0.0.1 --spring.data.redis.port=6379)
URL5=(--spring.data.redis.url=redis://127.0.0.1:6379/5)
URL0=(--spring.data.redis.url=redis://127.0.0.1:6379/0)
DB5=(--spring.data.redis.database=5)
DB0=(--spring.data.redis.database=0)

check() {
  local id="$1" label="$2" file="$3" pattern="$4"
  if grep -qF -- "$pattern" "$file"; then
    printf '  PASS  %-38s %s\n' "$id/$label" "$pattern"
    PASS=$((PASS + 1))
  else
    printf '  FAIL  %-38s %s (not found)\n' "$id/$label" "$pattern"
    FAIL=$((FAIL + 1))
  fi
}

run_case() {
  local id="$1" jar="$2" want="$3" want_type="$4"
  shift 4
  local log="$OUT/evidence/$id.log"
  java -jar "$jar" --probe.case="$id" --probe.expect="$want" "$@" > "$log" 2>&1
  local code=$?
  if [ "$code" -eq 0 ]; then
    printf '  PASS  %-38s exit=0\n' "$id/exit"
    PASS=$((PASS + 1))
  else
    printf '  FAIL  %-38s exit=%s\n' "$id/exit" "$code"
    FAIL=$((FAIL + 1))
  fi
  check "$id" "assert" "$log" "PROBE assert expected=$want result=PASS"
  check "$id" "factory" "$log" "PROBE factory.type=$want_type factory.database=$want"
}

echo "== cases: Spring Boot 4.1.2 =="
run_case 01-412-standalone-db5       "$J412" 5 RedisStandaloneConfiguration           "${STAND[@]}" "${DB5[@]}"
run_case 02-412-masterreplica-db5    "$J412" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${DB5[@]}"
run_case 03-412-masterreplica-db0    "$J412" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${DB0[@]}"
run_case 04-412-masterreplica-url5   "$J412" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${URL5[@]}"
run_case 05-412-masterreplica-url0   "$J412" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${URL0[@]}"
run_case 06-412-masterreplica-nodb   "$J412" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}"

echo "== cases: Spring Boot 4.2.0-RC2 =="
run_case 07-rc2-standalone-db5       "$JRC2" 5 RedisStandaloneConfiguration           "${STAND[@]}" "${DB5[@]}"
run_case 08-rc2-masterreplica-db5    "$JRC2" 5 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${DB5[@]}"
run_case 09-rc2-masterreplica-db0    "$JRC2" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${DB0[@]}"
run_case 10-rc2-masterreplica-url5   "$JRC2" 5 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${URL5[@]}"
run_case 11-rc2-masterreplica-url0   "$JRC2" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}" "${URL0[@]}"
run_case 12-rc2-masterreplica-nodb   "$JRC2" 0 RedisStaticMasterReplicaConfiguration "${NODES[@]}"

echo "== cross-cutting checks =="
check "01-412-standalone-db5" "version" "$OUT/evidence/01-412-standalone-db5.log" "boot=4.1.2"
check "07-rc2-standalone-db5" "version" "$OUT/evidence/07-rc2-standalone-db5.log" "boot=4.2.0-RC2"
check "02-412-masterreplica-db5" "configured" "$OUT/evidence/02-412-masterreplica-db5.log" "source.property.database=5"
check "08-rc2-masterreplica-db5" "configured" "$OUT/evidence/08-rc2-masterreplica-db5.log" "source.property.database=5"
check "04-412-masterreplica-url5" "url-source" "$OUT/evidence/04-412-masterreplica-url5.log" "source.url=redis://127.0.0.1:6379/5"
check "10-rc2-masterreplica-url5" "url-source" "$OUT/evidence/10-rc2-masterreplica-url5.log" "source.url=redis://127.0.0.1:6379/5"

# Silence: a wrong database must not produce a warning that names the database.
for id in 02-412-masterreplica-db5 08-rc2-masterreplica-db5; do
  if grep -iE 'WARN|ERROR' "$OUT/evidence/$id.log" | grep -qi 'database'; then
    printf '  FAIL  %-38s a warning mentions the database\n' "$id/silent"
    FAIL=$((FAIL + 1))
  else
    printf '  PASS  %-38s no warning mentions the database\n' "$id/silent"
    PASS=$((PASS + 1))
  fi
done

echo "== sanitize and publish evidence =="
python3 - "$OUT/evidence" "$EVID" <<'PY'
import pathlib, re, sys
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
for p in sorted(src.glob("*.log")):
    t = p.read_text()
    t = re.sub(r"/Users/[^/\s]+", "/Users/<redacted>", t)
    t = re.sub(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[^\s]*", "<timestamp>", t)
    (dst / p.name).write_text(t)
print(f"  wrote {len(list(dst.glob('*.log')))} sanitized logs to {dst}/")
PY

printf '\n  checks passed: %d   failed: %d\n' "$PASS" "$FAIL"
if [ "$FAIL" -eq 0 ]; then
  echo "  all cases match the recorded behaviour"
  exit 0
fi
echo "  recorded behaviour did not reproduce"
exit 1
