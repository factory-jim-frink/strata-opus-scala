#!/usr/bin/env python3
"""Fail when PMD complexity or unused-code findings exceed the tracked baseline."""

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
COMPLEXITY_RULESET = ROOT / "config/pmd/cyclomatic-complexity.xml"
DEAD_CODE_RULESET = ROOT / "config/pmd/dead-code.xml"
BASELINE = ROOT / "config/pmd/pmd-baseline.json"
PMD_NS = {"pmd": "http://pmd.sourceforge.net/report/2.0.0"}
COMPLEXITY_RULE = "CyclomaticComplexity"
DEAD_CODE_RULES = {"UnusedAssignment", "UnusedPrivateField", "UnusedPrivateMethod"}
COMPLEXITY = re.compile(r"complexity of (\d+)")
METHOD_SIGNATURE = re.compile(r"The method '([^']+)'")
DEAD_CODE_SYMBOLS = (
    re.compile(r"(?:field initializer for|initializer for variable|value assigned to variable) '([^']+)'"),
    re.compile(r"such as '([^']+)'"),
)


def expected_reports():
    modules = sorted(
        path for path in (ROOT / "modules").iterdir()
        if path.is_dir() and (path / "pom.xml").is_file()
    )
    return [module / "target/pmd.xml" for module in modules] + [
        ROOT / "examples/target/pmd.xml"
    ]


def read_thresholds():
    document = ET.parse(COMPLEXITY_RULESET).getroot()
    properties = {
        prop.get("name"): int(prop.get("value"))
        for prop in document.findall(".//{*}property")
        if prop.get("name") in ("methodReportLevel", "classReportLevel")
    }
    expected = {"methodReportLevel", "classReportLevel"}
    if set(properties) != expected:
        raise ValueError(f"PMD ruleset must define {sorted(expected)}")
    return properties


def read_rules():
    rules = set()
    for ruleset in (COMPLEXITY_RULESET, DEAD_CODE_RULESET):
        document = ET.parse(ruleset).getroot()
        for rule in document.findall(".//{*}rule"):
            reference = rule.get("ref")
            if reference:
                rules.add(reference.rsplit("/", 1)[-1])
    expected = {COMPLEXITY_RULE} | DEAD_CODE_RULES
    if rules != expected:
        raise ValueError(
            f"PMD rulesets define {sorted(rules)}, expected {sorted(expected)}"
        )
    return sorted(rules)


def read_dead_code_symbol(message):
    for pattern in DEAD_CODE_SYMBOLS:
        match = pattern.search(message)
        if match:
            return match.group(1)
    return None


def read_findings(rules):
    findings = {}
    errors = []
    for report in expected_reports():
        if not report.is_file():
            errors.append(f"Missing PMD report: {report.relative_to(ROOT)}")
            continue
        document = ET.parse(report).getroot()
        for error in document.findall(".//pmd:error", PMD_NS):
            errors.append(
                f"PMD analysis error in {error.get('filename')}: {error.get('msg')}"
            )
        for source in document.findall("pmd:file", PMD_NS):
            filename = Path(source.get("name", ""))
            if not filename.is_absolute():
                filename = ROOT / filename
            try:
                relative_filename = filename.resolve().relative_to(ROOT).as_posix()
            except ValueError:
                errors.append(f"PMD report contains a source outside the repo: {filename}")
                continue
            for violation in source.findall("pmd:violation", PMD_NS):
                rule = violation.get("rule")
                if rule not in rules:
                    continue
                message = (violation.text or "").strip()
                class_name = violation.get("class") or "<unknown-class>"
                if rule == COMPLEXITY_RULE:
                    method = violation.get("method")
                    # Aggregate class complexity remains visible in PMD reports, but
                    # only per-method complexity is regression-gated.
                    if not method:
                        continue
                    match = COMPLEXITY.search(message)
                    if not match:
                        errors.append(f"Cannot read complexity from: {message}")
                        continue
                    signature = METHOD_SIGNATURE.search(message)
                    method_identity = signature.group(1) if signature else method
                    identity = (
                        f"{rule}::{relative_filename}::{class_name}::{method_identity}"
                    )
                    if identity in findings:
                        errors.append(f"Duplicate PMD finding: {identity}")
                        continue
                    findings[identity] = int(match.group(1))
                    continue

                symbol = read_dead_code_symbol(message)
                if not symbol:
                    errors.append(f"Cannot identify unused symbol in: {message}")
                    continue
                method = violation.get("method") or "<class>"
                identity = f"{rule}::{relative_filename}::{class_name}::{method}::{symbol}"
                findings[identity] = findings.get(identity, 0) + 1
    return findings, errors


def rule_for(identity):
    return identity.split("::", 1)[0]


def summarize(findings):
    complexity_count = sum(rule_for(key) == COMPLEXITY_RULE for key in findings)
    dead_code_count = sum(
        value for key, value in findings.items() if rule_for(key) in DEAD_CODE_RULES
    )
    return complexity_count, dead_code_count


def write_baseline(findings, thresholds, rules):
    document = {
        "version": 1,
        "rules": rules,
        "thresholds": thresholds,
        "findings": dict(sorted(findings.items())),
    }
    BASELINE.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")
    complexity_count, dead_code_count = summarize(findings)
    print(
        f"Wrote baseline with {complexity_count} complexity methods and "
        f"{dead_code_count} existing dead-code findings to {BASELINE.relative_to(ROOT)}"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--update-baseline",
        action="store_true",
        help="replace the tracked baseline from the current PMD reports",
    )
    args = parser.parse_args()

    try:
        thresholds = read_thresholds()
        rules = read_rules()
        current, errors = read_findings(rules)
    except (OSError, ET.ParseError, ValueError) as exc:
        print(f"PMD baseline check failed: {exc}", file=sys.stderr)
        return 1

    if errors:
        for error in errors:
            print(f"PMD baseline check failed: {error}", file=sys.stderr)
        return 1
    if args.update_baseline:
        write_baseline(current, thresholds, rules)
        return 0
    if not BASELINE.is_file():
        print(
            f"Missing {BASELINE.relative_to(ROOT)}; generate and review it with "
            "`python3 config/pmd/check-pmd-baseline.py --update-baseline`.",
            file=sys.stderr,
        )
        return 1

    try:
        baseline = json.loads(BASELINE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"Cannot read PMD baseline: {exc}", file=sys.stderr)
        return 1
    if baseline.get("version") != 1 or baseline.get("rules") != rules:
        print("PMD rules differ from the tracked baseline; review it explicitly.", file=sys.stderr)
        return 1
    if baseline.get("thresholds") != thresholds:
        print(
            "PMD thresholds differ from the tracked baseline; review and update "
            "the baseline explicitly.",
            file=sys.stderr,
        )
        return 1

    previous = baseline.get("findings")
    if not isinstance(previous, dict) or any(
        not isinstance(value, int) for value in previous.values()
    ):
        print("PMD baseline findings must be a JSON object of integer values.", file=sys.stderr)
        return 1
    new_findings = sorted(set(current) - set(previous))
    increased = sorted(
        (identity, current[identity], previous[identity])
        for identity in set(current) & set(previous)
        if current[identity] > previous[identity]
    )
    if new_findings or increased:
        for identity in new_findings:
            label = "complexity" if rule_for(identity) == COMPLEXITY_RULE else "dead-code"
            print(f"New {label} finding ({current[identity]}): {identity}", file=sys.stderr)
        for identity, value, old_value in increased:
            if rule_for(identity) == COMPLEXITY_RULE:
                print(
                    f"Complexity increased from {old_value} to {value}: {identity}",
                    file=sys.stderr,
                )
            else:
                print(
                    f"Dead-code findings increased from {old_value} to {value}: {identity}",
                    file=sys.stderr,
                )
        return 1

    resolved = len(set(previous) - set(current))
    reduced = sum(current[key] < previous[key] for key in set(current) & set(previous))
    complexity_count, dead_code_count = summarize(current)
    print(
        f"PMD baseline passed: {complexity_count} complexity methods and "
        f"{dead_code_count} dead-code findings checked; {reduced} findings reduced "
        f"and {resolved} resolved."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
