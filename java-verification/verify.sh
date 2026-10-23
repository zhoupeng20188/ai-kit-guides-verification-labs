#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
mkdir -p verification-output
mvn -version > verification-output/environment.txt 2>&1
rm -rf verification-output/pass-reports verification-output/discovery-reports
mvn -B clean test > verification-output/pass.log 2>&1
cp -R target/surefire-reports verification-output/pass-reports
if mvn -B test -Dtest=UndiscoveredShould > verification-output/discovery.log 2>&1; then
  echo "Expected explicit selection to fail, but Maven returned success." >&2
  exit 1
else
  status=$?
  test "$status" -eq 1
fi
cp -R target/surefire-reports verification-output/discovery-reports
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
names = ('tests', 'failures', 'errors', 'skipped')
reports = list(Path('verification-output/pass-reports').glob('TEST-*.xml'))
totals = tuple(sum(int(ET.parse(p).getroot().get(k, '0')) for p in reports) for k in names)
assert totals == (9, 0, 0, 0), totals
root = ET.parse('verification-output/discovery-reports/TEST-guides.UndiscoveredShould.xml').getroot()
assert tuple(int(root.get(k, '0')) for k in names) == (1, 1, 0, 0)
assert 'Explicit selection discovered this intentionally failing test' in ET.tostring(root, encoding='unicode')
print('Verified: normal suite 9/0/0/0; explicit discovery 1/1/0/0 (expected failure).')
PY
