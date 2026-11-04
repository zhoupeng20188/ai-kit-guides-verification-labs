#!/usr/bin/env bash
# Reproduces every claim in /blog/spring-boot-4-2-properties-migrator-silent-gaps/.
# Each case is a separate JVM start of the same jar. The script exits 0 only when
# every case produces the exit code and the report presence recorded below.
#
# The build always runs clean: a stale file under target/classes is packaged into
# the jar and changes every result, which is how the first version of this script
# reported a false failure for the baseline case.
set -u

OUT=verification-output/evidence
JAR=target/migrator-lab-0.0.1-SNAPSHOT.jar
PASS=0
FAIL=0

need() { command -v "$1" >/dev/null 2>&1 || { echo "missing: $1"; exit 2; }; }
need java
need mvn

rm -rf verification-output
mkdir -p "$OUT"

echo "== build =="
mvn -B -q clean package -DskipTests || { echo "build failed"; exit 2; }

# run_case <id> <expected-exit> <expected-report yes|no> <env-prefix or ""> <arguments...>
run_case() {
  local id="$1" want="$2" want_report="$3" env_prefix="$4"
  shift 4

  if [ -n "$env_prefix" ]; then
    env $env_prefix java -jar "$JAR" --spring.main.web-application-type=none "$@" > "$OUT/$id.log" 2>&1
  else
    java -jar "$JAR" --spring.main.web-application-type=none "$@" > "$OUT/$id.log" 2>&1
  fi
  local code=$?
  local report=no
  grep -q "The use of configuration keys" "$OUT/$id.log" && report=yes

  if [ "$code" = "$want" ] && [ "$report" = "$want_report" ]; then
    printf '  PASS  %-28s exit=%s report=%s\n' "$id" "$code" "$report"
    PASS=$((PASS + 1))
  else
    printf '  FAIL  %-28s exit=%s (want %s) report=%s (want %s)\n' \
      "$id" "$code" "$want" "$report" "$want_report"
    FAIL=$((FAIL + 1))
  fi
}

# check_line <id> <label> <expected substring>
check_line() {
  local id="$1" label="$2" want="$3"
  if grep -qF -- "$want" "$OUT/$id.log"; then
    printf '  PASS  %-28s %s\n' "$id/$label" "$want"
    PASS=$((PASS + 1))
  else
    printf '  FAIL  %-28s %s (not found)\n' "$id/$label" "$want"
    FAIL=$((FAIL + 1))
  fi
}

echo "== cases =="

# The two key categories the migrator knows about.
RENAMED=--spring.resources.add-mappings=false
REMOVED=--spring.banner.image.width=76

run_case 00-baseline            0 no  ""
run_case 01-renamed-never       0 yes "" $RENAMED
run_case 02-renamed-on-error    0 yes "" $RENAMED --spring.properties.migrator.fail=on-error
run_case 03-renamed-on-warning  1 yes "" $RENAMED --spring.properties.migrator.fail=on-warning
run_case 04-removed-never       0 yes "" $REMOVED
run_case 05-removed-on-error    1 yes "" $REMOVED --spring.properties.migrator.fail=on-error
run_case 06-removed-on-warning  1 yes "" $REMOVED --spring.properties.migrator.fail=on-warning

# Gap 4: a key that is not in the metadata, and a real key with no consumer.
run_case 07-invented-key        0 no  "" --spring.web.resource.add-mappings=false --spring.properties.migrator.fail=on-warning
run_case 08-unbound-key         0 no  "" --spring.datasource.hikari.maxLifetime=60000 --spring.properties.migrator.fail=on-warning

# Gap 3: the same old key arriving through different loaders.
# 09 activates a profile with no matching profile file, so only @PropertySource supplies the key.
run_case 09-propertysource      0 no  "" --spring.profiles.active=psource --spring.properties.migrator.fail=on-warning
run_case 10-profile-file        1 yes "" --spring.profiles.active=legacy --spring.properties.migrator.fail=on-warning
run_case 11-config-import       1 yes "" --spring.config.import=classpath:imported.properties --spring.properties.migrator.fail=on-warning
run_case 12-env-var-old-key     1 yes "SPRING_RESOURCES_ADD_MAPPINGS=false" --spring.properties.migrator.fail=on-warning

# The switch's own value is an enum; a misspelling fails at binding, before the migration check.
run_case 13-typo-fail-value     1 no  "" $REMOVED --spring.properties.migrator.fail=on-warn

# A mixed set still stops the process under on-error, because the removed key is error level.
run_case 14-mixed-on-error      1 yes "" $RENAMED $REMOVED --spring.properties.migrator.fail=on-error
# The same legacy key in YAML, reaching the environment through config data.
run_case 15-yaml-old-key        1 yes "" --spring.config.additional-location=file:./fixtures/legacy.yml --spring.properties.migrator.fail=on-warning

echo "== probes =="

# What the environment and the bound object hold, for the same key via two loaders.
run_case 16-probe-propertysource 0 no "" --spring.profiles.active=psource --probe=true
run_case 17-probe-commandline    0 yes "" $RENAMED --probe=true

# The @PropertySource copy keeps the old name, never gets the new name, and the bean keeps the default.
check_line 16-probe-propertysource "old-name" "PROBE old-name   spring.resources.add-mappings      = false"
check_line 16-probe-propertysource "new-name" "PROBE new-name   spring.web.resources.add-mappings  = null"
check_line 16-probe-propertysource "bound"    "PROBE bound      spring.web.resources.addMappings   = true"
# The command-line copy is renamed by the migrator before the bean is created.
check_line 17-probe-commandline "new-name" "PROBE new-name   spring.web.resources.add-mappings  = false"
check_line 17-probe-commandline "bound"    "PROBE bound      spring.web.resources.addMappings   = false"

echo "== summary =="
printf '  checks passed: %s   failed: %s\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ] || exit 1
echo "  all cases match the recorded behaviour"
