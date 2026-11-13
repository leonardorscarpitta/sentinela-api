"""PII stage (Sentinela Cidadã): personal-data mapping and taint flows with Semgrep CE and the rules in rules/pii.

What leaves this module is the normalized finding only: rule, file, line, category/sink and the trace as
path + line steps. Matched values, code snippets and metavariable contents stay inside the runner:
  * literal rules with a check-digit validator (cpf, cns, luhn) are verified by reading the match locally and are
    dropped when the digits are wrong;
  * Semgrep CE's CLI no longer prints dataflow traces in its JSON output, so for each file with a flow finding the
    engine (semgrep-core, same rules) is run on that file alone to recover the trace locations.
"""
import json
import os
import re
import shutil
from pathlib import Path

from .base import ToolError, ToolResult, finding, location, read_json, relative, run, severity, summary
from ..sanitize import clip

RULES_DIR = Path(os.environ.get("ARTEMIS_RULES_DIR", "/opt/artemis/rules")) / "pii"
# The flow rules with YAML anchors expanded (built into the image): semgrep-core does not resolve anchors.
CORE_RULES_DIR = Path(os.environ.get("ARTEMIS_CORE_RULES_DIR", "/opt/artemis/rules-core"))
CATEGORIES = {"identification", "health", "credential", "contact", "financial"}
SINKS = {"log", "httpResponse", "externalApi", "file", "exception"}
# Files whose flow traces are recovered (one engine run each) and steps kept per trace.
MAX_TRACED_FILES = 200
MAX_TRACE_STEPS = 12
MAX_MATCH_BYTES = 256

FLOW_RULES = {
    ".java": ("java", "flow-java.json"),
    ".py": ("python", "flow-python.json"),
    ".js": ("js", "flow-javascript.json"),
    ".jsx": ("js", "flow-javascript.json"),
    ".mjs": ("js", "flow-javascript.json"),
    ".cjs": ("js", "flow-javascript.json"),
    ".ts": ("ts", "flow-javascript.json"),
    ".tsx": ("ts", "flow-javascript.json"),
}

# Public test card numbers (payment providers' documentation): never personal data.
TEST_CARDS = {
    "4111111111111111", "4242424242424242", "4012888888881881", "4000056655665556", "5555555555554444",
    "5105105105105100", "2223003122003222", "5200828282828210", "6011111111111117", "6011000990139424",
}


def scan(src: Path, out: Path, timeout: int) -> ToolResult:
    report = out / "pii.json"
    run(
        ["semgrep", "scan", "--config", str(RULES_DIR), "--json", "--output", str(report), "--metrics=off",
         "--disable-version-check", "--quiet", "--timeout", "30", "--max-target-bytes", "1000000",
         "--jobs", "1", "--max-memory", "1200", str(src)],
        timeout, out, ok_codes=(0, 1),
    )
    data = read_json(report, {}) or {}
    root = src.resolve()
    findings, flows, dropped = [], [], 0
    for result in data.get("results", []):
        metadata = result.get("extra", {}).get("metadata", {})
        privacy = metadata.get("sentinela") or {}
        category, kind, sink = privacy.get("category"), privacy.get("type"), privacy.get("sink")
        if category not in CATEGORIES or kind not in ("mapping", "flow") or (kind == "flow" and sink not in SINKS):
            continue
        validator = privacy.get("validator")
        if validator and not _valid(validator, _matched_text(result, root)):
            dropped += 1
            continue
        rule = (result.get("check_id") or "rule").rsplit(".", 1)[-1]
        path = relative(result.get("path"), src)
        line = result.get("start", {}).get("line")
        item = finding(
            "privacy", rule, metadata.get("title") or rule, result.get("extra", {}).get("message", ""),
            severity(metadata.get("artemis-severity"), "medium"),
            cwe=_cwe(metadata.get("cwe")),
            location=location(path, line),
            privacy={"category": category, "type": kind, "sink": sink if kind == "flow" else None, "trace": []},
        )
        findings.append(item)
        if kind == "flow":
            flows.append((item, result))

    traced = _attach_traces(flows, src, out, timeout)
    log = summary("Semgrep", findings)
    mapped = sum(1 for f in findings if f["privacy"]["type"] == "mapping")
    log += f"\n[PII] {mapped} mapped location(s), {len(flows)} flow(s) to insecure outputs ({traced} with trace)"
    if dropped:
        log += f"\n[PII] {dropped} literal match(es) dropped by check-digit validation"
    errors = [e for e in data.get("errors", []) if e.get("level") == "error"]
    if errors:
        log += f"\n[Semgrep] {len(errors)} file(s) could not be analyzed"
    return ToolResult(findings, log)


def _attach_traces(flows: list, src: Path, out: Path, timeout: int) -> int:
    """Runs the engine on each file that has flow findings and copies the trace locations onto them."""
    core = _semgrep_core()
    if not core or not flows:
        return 0
    by_file: dict[str, list] = {}
    for item, result in flows:
        by_file.setdefault(result.get("path"), []).append((item, result))
    traced = 0
    for index, (path, items) in enumerate(by_file.items()):
        rules = FLOW_RULES.get(Path(path).suffix.lower())
        if index >= MAX_TRACED_FILES or rules is None:
            continue
        lang, rules_file = rules
        if not (CORE_RULES_DIR / rules_file).is_file():
            continue
        try:
            proc = run([core, "-rules", str(CORE_RULES_DIR / rules_file), "-lang", lang, "-json", "-timeout", "30",
                        "-max_memory", "1200", path], min(timeout, 120), out)
        except ToolError:
            continue
        traces = _core_traces(proc.stdout or "")
        for item, result in items:
            rule = (result.get("check_id") or "").rsplit(".", 1)[-1]
            start = result.get("start", {})
            trace = traces.get((rule, start.get("line"), start.get("col")))
            steps = _steps(trace, src) if trace else []
            if not steps:
                steps = [{"role": "sink", "path": item["location"]["path"], "line": item["location"]["line"]}]
            else:
                traced += 1
            item["privacy"]["trace"] = steps
    return traced


def _core_traces(stdout: str) -> dict:
    start = stdout.find("{")
    if start < 0:
        return {}
    try:
        data = json.loads(stdout[start:])
    except json.JSONDecodeError:
        return {}
    traces = {}
    for result in data.get("results", []):
        trace = result.get("extra", {}).get("dataflow_trace")
        if trace:
            begin = result.get("start", {})
            traces[(result.get("check_id"), begin.get("line"), begin.get("col"))] = trace
    return traces


def _steps(trace: dict, src: Path) -> list[dict]:
    """source → intermediate variables → sink, as path + line only (the engine's code excerpts are discarded)."""
    steps = []

    def add(role: str, loc: dict | None) -> None:
        if not loc:
            return
        step = {"role": role, "path": clip(relative(loc.get("path"), src), 500),
                "line": loc.get("start", {}).get("line")}
        if steps and steps[-1]["path"] == step["path"] and steps[-1]["line"] == step["line"] and role == "propagation":
            return
        steps.append(step)

    add("source", _first_location(trace.get("taint_source")))
    for var in trace.get("intermediate_vars") or []:
        add("propagation", var.get("location"))
    add("sink", _first_location(trace.get("taint_sink")))
    if len(steps) > MAX_TRACE_STEPS:
        steps = steps[: MAX_TRACE_STEPS - 1] + steps[-1:]
    return [s for s in steps if s["path"]]


def _first_location(node) -> dict | None:
    """Trace nodes are ["CliLoc", [location, code]] or ["CliCall", [...]]: the first location found, depth first."""
    if isinstance(node, dict):
        if "path" in node and "start" in node:
            return node
        for value in node.values():
            found = _first_location(value)
            if found:
                return found
    elif isinstance(node, list):
        for value in node:
            found = _first_location(value)
            if found:
                return found
    return None


def _semgrep_core() -> str | None:
    found = shutil.which("semgrep-core")
    if found:
        return found
    for candidate in Path("/opt/semgrep/lib").glob("python3*/site-packages/semgrep/bin/semgrep-core"):
        return str(candidate)
    return None


def _matched_text(result: dict, root: Path) -> str:
    """The matched characters, read locally for validation only (never logged or reported)."""
    begin = result.get("start", {}).get("offset")
    end = result.get("end", {}).get("offset")
    if not isinstance(begin, int) or not isinstance(end, int) or not 0 <= end - begin <= MAX_MATCH_BYTES:
        return ""
    path = Path(result.get("path") or "").resolve()
    if not path.is_relative_to(root) or not path.is_file():
        return ""
    try:
        with open(path, "rb") as handle:
            handle.seek(begin)
            return handle.read(end - begin).decode("utf-8", "ignore")
    except OSError:
        return ""


def _valid(validator: str, text: str) -> bool:
    digits = re.sub(r"\D", "", text)
    if validator == "cpf":
        return valid_cpf(digits[-11:])
    if validator == "cns":
        return valid_cns(digits[-15:])
    if validator == "luhn":
        return valid_card(digits)
    return False


def valid_cpf(digits: str) -> bool:
    if len(digits) != 11 or len(set(digits)) == 1:
        return False
    numbers = [int(d) for d in digits]
    for size in (9, 10):
        total = sum(n * w for n, w in zip(numbers[:size], range(size + 1, 1, -1)))
        check = (total * 10) % 11 % 10
        if check != numbers[size]:
            return False
    return True


def valid_cns(digits: str) -> bool:
    """Cartão Nacional de Saúde: 15 digits starting with 1, 2, 7, 8 or 9; weighted sum (15..1) divisible by 11."""
    if len(digits) != 15 or digits[0] not in "12789" or len(set(digits)) == 1:
        return False
    return sum(int(d) * w for d, w in zip(digits, range(15, 0, -1))) % 11 == 0


def valid_card(digits: str) -> bool:
    if not 13 <= len(digits) <= 19 or len(set(digits)) == 1 or digits in TEST_CARDS:
        return False
    total = 0
    for index, d in enumerate(reversed(digits)):
        n = int(d)
        if index % 2:
            n = n * 2 - 9 if n > 4 else n * 2
        total += n
    return total % 10 == 0


def _cwe(value) -> str | None:
    if isinstance(value, list):
        value = value[0] if value else None
    return value.split(":")[0].strip() if isinstance(value, str) else None
