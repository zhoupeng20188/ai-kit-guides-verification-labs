#!/usr/bin/env bash
# Measure what a trustbut Maven receipt can and cannot prove about JUnit 6 tests.
#
# Usage:
#   JAVA_HOME=/path/to/jdk-17 TRUSTBUT_BIN=/path/to/trustbut bash verify.sh
#
# TRUSTBUT_BIN is optional; without it the script uses `npx --yes trustbut@0.7.0`.
# The lab builds a temporary git repository, so nothing here modifies this repo.
set -uo pipefail

LAB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVIDENCE="$LAB_DIR/evidence"

if [ -z "${JAVA_HOME:-}" ]; then
  echo "ERROR: set JAVA_HOME to a JDK 17 installation." >&2
  exit 2
fi
export PATH="$JAVA_HOME/bin:$PATH"

if [ -n "${TRUSTBUT_BIN:-}" ]; then
  TBC=("$TRUSTBUT_BIN")
else
  TBC=(npx --yes trustbut@0.7.0)
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$EVIDENCE"
rm -f "$EVIDENCE"/*.md "$EVIDENCE"/*.txt "$EVIDENCE"/*.json "$EVIDENCE"/summary.tsv

cp "$LAB_DIR/pom.xml" "$WORK/pom.xml"
cp -R "$LAB_DIR/src" "$WORK/src"
cd "$WORK" || exit 2

# Without this, Maven's target/ makes the working tree dirty and trustbut
# reports "Uncommitted changes are not included in this receipt".
printf 'target/\n.trustbut.json\n' > .gitignore
git init -q -b main
git config user.email lab@example.invalid
git config user.name "Verification Lab"
git add -A
git commit -q -m "base: boundary rule with an off-by-one"

# ---------------------------------------------------------------- cases ---
case_control_red() {
cat > src/test/java/demo/PriceTest.java <<'JAVA'
package demo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class PriceTest {
    @Test void aboveBoundaryQualifies() { assertTrue(Price.qualifies(10001)); }
    @Test void boundaryIsIncluded() { assertTrue(Price.qualifies(10000)); }
}
JAVA
}

case_proved() {
cat > src/main/java/demo/Price.java <<'JAVA'
package demo;
public final class Price {
    private Price() { }
    public static boolean qualifies(int cents) { return cents >= 10000; }
}
JAVA
cat > src/test/java/demo/PriceTest.java <<'JAVA'
package demo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class PriceTest {
    @Test void aboveBoundaryQualifies() { assertTrue(Price.qualifies(10001)); }
    @Test void boundaryIsIncluded() { assertTrue(Price.qualifies(10000)); }
}
JAVA
}

case_should() {
cat > src/test/java/demo/PriceShould.java <<'JAVA'
package demo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class PriceShould {
    @Test void boundaryIsIncluded() { assertTrue(Price.qualifies(10000)); }
}
JAVA
}

case_it() {
cat > src/test/java/demo/PriceIT.java <<'JAVA'
package demo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class PriceIT {
    @Test void boundaryIsIncluded() { assertTrue(Price.qualifies(10000)); }
}
JAVA
}

case_disabled() {
cat > src/test/java/demo/PriceDisabledTest.java <<'JAVA'
package demo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
@Disabled("pending fixture")
class PriceDisabledTest {
    @Test void boundaryIsIncluded() { assertTrue(Price.qualifies(10000)); }
}
JAVA
}

case_weakened() {
cat > src/test/java/demo/PriceWeakenedTest.java <<'JAVA'
package demo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class PriceWeakenedTest {
    @Test void boundaryIsIncluded() { assertTrue(true); }
}
JAVA
}

case_code_only() {
cat > src/main/java/demo/Price.java <<'JAVA'
package demo;
public final class Price {
    private Price() { }
    public static boolean qualifies(int cents) { return cents >= 10000; }
}
JAVA
}

expected_for() {
  case "$1" in
    01-control-red)     echo "FAIL" ;;
    02-proved)          echo "PASS" ;;
    03-should)          echo "PASS" ;;
    04-it)              echo "PASS" ;;
    05-disabled)        echo "WARN" ;;
    06-disabled-strict) echo "WARN" ;;
    # 07 is PASS, not WARN: the receipt records "1 pass without it" but raises
    # no finding, so a passing no-op test suppresses the "nothing proves" WARN.
    07-weakened)        echo "PASS" ;;
    08-code-only)       echo "WARN" ;;
    09-weakened-strict) echo "PASS" ;;
    10-should-strict)   echo "PASS" ;;
  esac
}

printf 'case\tmvn_exit\tmvn_tests\tverdict\texit\texpected\tmatch\n' > "$EVIDENCE/summary.tsv"
PASSED=0
FAILED=0

run_case() {
  name=$1; maker=$2; strict=$3
  git checkout -q main
  git checkout -q -B "case-$name"
  "$maker"
  git add -A
  git commit -q -m "case $name"

  mvn -B test > "$WORK/mvn-$name.log" 2>&1
  mvn_exit=$?
  grep -E "Tests run:|BUILD (SUCCESS|FAILURE)|No tests to run" "$WORK/mvn-$name.log" \
    | tail -6 > "$EVIDENCE/$name.mvn.txt"
  mvn_tests=$(grep -cE '^\[(INFO|WARNING|ERROR)\] Tests run:' "$WORK/mvn-$name.log")

  if [ "$strict" = "strict" ]; then
    "${TBC[@]}" verify -b main --strict \
        --receipt-md "$EVIDENCE/$name.receipt.md" \
        --receipt-json "$EVIDENCE/$name.receipt.json" \
        > /dev/null 2> "$WORK/tb-$name.err"
  else
    "${TBC[@]}" verify -b main \
        --receipt-md "$EVIDENCE/$name.receipt.md" \
        --receipt-json "$EVIDENCE/$name.receipt.json" \
        > /dev/null 2> "$WORK/tb-$name.err"
  fi
  tb_exit=$?

  verdict=$(grep -m1 -oE 'Receipt: (PASS|WARN|FAIL)' "$EVIDENCE/$name.receipt.md" | awk '{print $2}')
  verdict=${verdict:-NONE}
  expected=$(expected_for "$name")
  if [ "$verdict" = "$expected" ]; then
    match="yes"; PASSED=$((PASSED + 1))
  else
    match="NO"; FAILED=$((FAILED + 1))
  fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$name" "$mvn_exit" "$mvn_tests" "$verdict" "$tb_exit" "$expected" "$match" \
    >> "$EVIDENCE/summary.tsv"
  if [ -s "$WORK/tb-$name.err" ]; then
    cp "$WORK/tb-$name.err" "$EVIDENCE/$name.stderr.txt"
  fi
}

run_case 01-control-red     case_control_red  no
run_case 02-proved          case_proved       no
run_case 03-should          case_should       no
run_case 04-it              case_it           no
run_case 05-disabled        case_disabled     no
run_case 06-disabled-strict case_disabled     strict
run_case 07-weakened        case_weakened     no
run_case 08-code-only       case_code_only    no
run_case 09-weakened-strict case_weakened     strict
run_case 10-should-strict   case_should       strict

column -t -s $'\t' "$EVIDENCE/summary.tsv"
echo
echo "toolchain: java=$(java -version 2>&1 | head -1) | maven=$(mvn -v 2>&1 | head -1) | node=$(node -v) | trustbut=$("${TBC[@]}" --version)"
echo "checks passed: $PASSED failed: $FAILED"
