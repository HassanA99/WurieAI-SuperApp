"""Deployment drift checker.

Compares the contract this repository currently defines against the running deployment,
so a stale production revision cannot hide behind a green health check.

Usage:
    uv run python -m app.deployment_check https://wurieai-superapp.onrender.com

Exit codes: 0 = in sync, 1 = drift detected, 2 = deployment unreachable.
"""

from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

# Aliases the mobile client depends on; losing one breaks already-shipped builds.
REQUIRED_CHAT_ALIASES = ("/chat", "/api/chat", "/api/v1/chat")

# /health must stay flat and string-valued: the Android client declares Map<String, String>.
REQUIRED_HEALTH_FIELDS = (
    "status",
    "agent_available",
    "agent_model",
    "agent_fallback",
    "agent_reason",
    "firebase_admin",
    "storage",
    "error_tracking",
)

IGNORED_PATH_PREFIXES = ("/openapi", "/docs", "/redoc")


def expected_routes() -> list[str]:
    """Return the routes the current code will expose once it is deployed."""
    from app.main import app

    routes: set[str] = set()
    for route in app.routes:
        methods = getattr(route, "methods", None)
        path = getattr(route, "path", "") or ""
        if methods and path and not path.startswith(IGNORED_PATH_PREFIXES):
            routes.add(path)
    return sorted(routes)


def compare_routes(expected, actual) -> dict:
    """Compare route inventories without touching the network."""
    expected_set, actual_set = set(expected), set(actual)
    missing = sorted(expected_set - actual_set)
    unexpected = sorted(actual_set - expected_set)
    return {
        "missing": missing,
        "unexpected": unexpected,
        "missing_chat_aliases": sorted(set(REQUIRED_CHAT_ALIASES) & set(missing)),
        "in_sync": not missing and not unexpected,
    }


def compare_health(payload) -> dict:
    """Verify the flat, string-valued /health contract."""
    if not isinstance(payload, dict):
        return {"missing": list(REQUIRED_HEALTH_FIELDS), "wrong_type": [], "in_sync": False}

    missing = [field for field in REQUIRED_HEALTH_FIELDS if field not in payload]
    wrong_type = [
        field
        for field in REQUIRED_HEALTH_FIELDS
        if field in payload and not isinstance(payload[field], str)
    ]
    return {
        "missing": missing,
        "wrong_type": wrong_type,
        "in_sync": not missing and not wrong_type,
    }


def fetch_json(url: str, timeout: float = 30.0) -> dict:
    request = urllib.request.Request(
        url, headers={"User-Agent": "wurieai-deployment-check/1.0"}
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def check(base_url: str, timeout: float = 30.0) -> dict:
    """Compare the live deployment against the current code."""
    base = base_url.rstrip("/")
    live_paths = sorted(fetch_json(f"{base}/openapi.json", timeout).get("paths", {}) or {})
    routes = compare_routes(expected_routes(), live_paths)

    health_payload = fetch_json(f"{base}/health", timeout)
    health = compare_health(health_payload)

    return {
        "base_url": base,
        "routes": routes,
        "health": health,
        "health_payload": health_payload,
        "live_routes": live_paths,
        "in_sync": routes["in_sync"] and health["in_sync"],
    }


def _print_report(report: dict) -> None:
    routes = report["routes"]
    health = report["health"]

    print(f"deployment check: {report['base_url']}")
    print(f"  live routes ({len(report['live_routes'])}):")
    for path in report["live_routes"]:
        print(f"    {path}")

    if routes["missing"]:
        print(f"  MISSING routes (in code, not deployed): {', '.join(routes['missing'])}")
    if routes["missing_chat_aliases"]:
        print(
            "  BREAKING: deployed revision is missing chat aliases the mobile client uses: "
            f"{', '.join(routes['missing_chat_aliases'])}"
        )
    if routes["unexpected"]:
        print(f"  UNEXPECTED routes (deployed, not in code): {', '.join(routes['unexpected'])}")

    print("  /health payload:")
    for key, value in report["health_payload"].items():
        print(f"    {key} = {value}")
    if health["missing"]:
        print(f"  HEALTH CONTRACT missing fields: {', '.join(health['missing'])}")
    if health["wrong_type"]:
        print(f"  HEALTH CONTRACT non-string fields: {', '.join(health['wrong_type'])}")

    print(f"  result: {'IN SYNC' if report['in_sync'] else 'DRIFT DETECTED'}")


def main(argv=None) -> int:
    args = list(sys.argv[1:] if argv is None else argv)
    if not args:
        print(__doc__)
        return 2

    base_url = args[0]
    try:
        report = check(base_url)
    except (urllib.error.URLError, TimeoutError, ValueError, OSError) as exc:
        print(f"UNREACHABLE {base_url}: {exc}")
        return 2

    _print_report(report)
    return 0 if report["in_sync"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
