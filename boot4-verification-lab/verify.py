#!/usr/bin/env python3
"""Run the two compatibility profiles and verify the intentional discovery failure."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "verification-output"
EVIDENCE = OUTPUT / "evidence"
ATTRS = ("tests", "failures", "errors", "skipped")
EXPECTED = {"SecurityContractTest": 6, "NetworkContractTest": 1, "DatabaseActivationTest": 1, "FlywayHistoryTest": 3}


def sanitize(value):
    value = value.replace(str(ROOT), "<LAB_ROOT>").replace(str(Path.home()), "<HOME>")
    value = re.sub(r"/(?:private/)?var/folders/[^\s,;\]]+", "<TEMP>", value)
    return re.sub(r"started by \S+ in", "started by <USER> in", value)


def execute(label, arguments, expected_status=0):
    command = ["mvn", "-B", *arguments]
    print("Running:", " ".join(command), flush=True)
    with (OUTPUT / (label + ".log")).open("w") as log:
        completed = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    text = (OUTPUT / (label + ".log")).read_text()
    (EVIDENCE / (label + ".log")).write_text(sanitize(text))
    if completed.returncode != expected_status:
        print(text[-7000:], file=sys.stderr)
        raise RuntimeError(f"{label}: expected exit {expected_status}, got {completed.returncode}")
    return {"command": " ".join(command), "exit": completed.returncode}


def reports(label, discovery=False, error_dispatch=False):
    destination = EVIDENCE / label
    destination.mkdir()
    totals = [0, 0, 0, 0]
    selected = {}
    versions = {}
    for path in sorted((ROOT / "target/surefire-reports").glob("TEST-*.xml")):
        if discovery and path.name != "TEST-guides.bootlab.UndiscoveredShould.xml":
            continue
        if error_dispatch and path.name != "TEST-guides.bootlab.NetworkContractTest.xml":
            continue
        element = ET.parse(path).getroot()
        properties = element.find("properties")
        if properties is not None:
            props = {p.get("name"): p.get("value", "") for p in properties}
            versions["java"] = props.get("java.version")
            classpath = props.get("java.class.path", "").replace("\\", "/")
            for artifact in ["spring-boot", "spring-security-core", "flyway-core", "h2", "junit-jupiter-api"]:
                match = re.search(r"/" + re.escape(artifact) + r"/([^/]+)/", classpath)
                if match:
                    versions[artifact] = match.group(1)
            element.remove(properties)
        values = [int(element.get(key, "0")) for key in ATTRS]
        totals = [a + b for a, b in zip(totals, values)]
        selected[element.get("name", "").split(".")[-1]] = values[0]
        for node in element.iter():
            if node.text:
                node.text = sanitize(node.text)
            if node.tail:
                node.tail = sanitize(node.tail)
            node.attrib = {key: sanitize(value) for key, value in node.attrib.items()}
        xml = ET.tostring(element, encoding="unicode")
        (destination / path.name).write_text(xml)
    if discovery or error_dispatch:
        assert totals == [1, 1, 0, 0], totals
        if discovery:
            xml = (destination / "TEST-guides.bootlab.UndiscoveredShould.xml").read_text()
            assert "DISCOVERY_SENTINEL" in xml
        else:
            xml = (destination / "TEST-guides.bootlab.NetworkContractTest.xml").read_text()
            assert "basic_post_without_csrf=401" in xml
            assert "expected: &lt;403&gt; but was: &lt;401&gt;" in xml
    else:
        assert selected == EXPECTED, selected
        assert totals == [11, 0, 0, 0], totals
    return {**dict(zip(ATTRS, totals)), "versions": versions}


def main():
    OUTPUT.mkdir(exist_ok=True)
    if EVIDENCE.exists():
        shutil.rmtree(EVIDENCE)
    EVIDENCE.mkdir()
    environment = subprocess.run(["mvn", "-version"], cwd=ROOT, text=True,
                                 stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=True).stdout
    (EVIDENCE / "environment.txt").write_text(sanitize(environment))
    results = {}
    for profile in ["boot3", "boot4"]:
        outcome = execute(profile, ["-P" + profile, "clean", "test"])
        results[profile] = {**outcome, **reports(profile)}
    outcome = execute("discovery", ["-Pboot4", "test", "-Dtest=UndiscoveredShould"], 1)
    results["discovery"] = {**outcome, **reports("discovery", discovery=True)}
    outcome = execute("error-dispatch", ["-Pboot4", "test", "-Dtest=NetworkContractTest",
                      "-Dlab.allow-error-dispatch=false", "-Dlogging.level.org.springframework.security=TRACE"], 1)
    results["error-dispatch"] = {**outcome, **reports("error-dispatch", error_dispatch=True)}
    record = {
        "executed_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "os": platform.system(),
        "architecture": platform.machine(),
        "results": results,
        "scope": "Synthetic MockMvc, loopback HTTP and H2 fixture; no AI-agent benchmark or external database.",
        "source_sha256": {
            str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted([ROOT / "pom.xml", *ROOT.glob("src/**/*")]) if path.is_file()
        },
    }
    (EVIDENCE / "results.json").write_text(json.dumps(record, indent=2) + "\n")
    print("Verified: both profiles passed 11 checks; discovery and error-dispatch faults failed as expected.")
    print("Sanitized evidence:", EVIDENCE)


if __name__ == "__main__":
    main()
