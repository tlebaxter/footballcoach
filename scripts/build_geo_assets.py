#!/usr/bin/env python3
"""Build schools_geo.json (CFBD) and places_10k.json (Census Gaz + ACS)."""

from __future__ import annotations

import csv
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCAL_PROPS = ROOT / "local.properties"
GAZ = ROOT / "2025_Gaz_place_national.txt"
SCHOOLS_CSV = ROOT / "app/src/main/assets/fbs_2026_classes.csv"
OUT_SCHOOLS = ROOT / "sim/src/main/resources/schools_geo.json"
OUT_PLACES = ROOT / "sim/src/main/resources/places_10k.json"

CFBD_TEAMS = "https://api.collegefootballdata.com/teams"
CFBD_VENUES = "https://api.collegefootballdata.com/venues"
# Free population file (no API key). SUMLEV 162 = incorporated place.
SUBEST_URL = (
    "https://www2.census.gov/programs-surveys/popest/datasets/"
    "2020-2024/cities/totals/sub-est2024.csv"
)
SUBEST_CACHE = ROOT / "scripts" / "cache" / "sub-est2024.csv"

# Manual CFBD school-name overrides when CSV name != CFBD school
NAME_OVERRIDES: dict[str, str] = {
    "Ole Miss": "Mississippi",
    "Miami": "Miami",
    "Miami (Ohio)": "Miami (OH)",
    "ULM": "UL Monroe",
    "Louisiana": "Louisiana",
    "UConn": "Connecticut",
    "UMass": "Massachusetts",
    "UTSA": "UTSA",
    "UTEP": "UTEP",
    "UCF": "UCF",
    "USF": "South Florida",
    "UCLA": "UCLA",
    "USC": "USC",
    "SMU": "SMU",
    "TCU": "TCU",
    "BYU": "BYU",
    "LSU": "LSU",
    "NC State": "NC State",
    "Pittsburgh": "Pittsburgh",
    "Southern Miss": "Southern Miss",
    "Appalachian State": "App State",
    "FIU": "Florida International",
    "WKU": "Western Kentucky",
    "San Jose State": "San José State",
    "Connecticut": "Connecticut",
}

# Hardcoded lat/lon for schools CFBD may miss (FCS/new/fictional in our CSV)
HARDCODED: dict[str, dict[str, object]] = {
    "MER": {
        "name": "Merrimack",
        "lat": 42.6681,
        "lon": -71.1231,
        "city": "North Andover",
        "state": "MA",
        "venue": "Duane Stadium",
    },
    "BEN": {
        "name": "Bentley",
        "lat": 42.3875,
        "lon": -71.2200,
        "city": "Waltham",
        "state": "MA",
        "venue": "Bentley Athletic Field",
    },
    "NDS": {
        "name": "North Dakota State",
        "lat": 46.8930,
        "lon": -96.8018,
        "city": "Fargo",
        "state": "ND",
        "venue": "Fargodome",
    },
    "SAC": {
        "name": "Sacramento State",
        "lat": 38.5575,
        "lon": -121.4220,
        "city": "Sacramento",
        "state": "CA",
        "venue": "Hornet Stadium",
    },
}


def read_cfbd_key() -> str:
    if not LOCAL_PROPS.exists():
        raise SystemExit(f"Missing {LOCAL_PROPS}")
    for line in LOCAL_PROPS.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("cfbd=") or line.startswith("cfbd.api.key="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("No cfbd= key in local.properties")


def http_get_json(url: str, headers: dict[str, str] | None = None) -> object:
    req = urllib.request.Request(url, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {e.code} for {url}: {body[:300]}") from e


def normalize(name: str) -> str:
    s = name.lower().strip()
    s = s.replace("&", "and")
    s = re.sub(r"[^a-z0-9]+", "", s)
    return s


def build_schools(key: str) -> list[dict[str, object]]:
    headers = {"Authorization": f"Bearer {key}", "Accept": "application/json"}
    teams = http_get_json(CFBD_TEAMS, headers)
    venues = http_get_json(CFBD_VENUES, headers)
    assert isinstance(teams, list) and isinstance(venues, list)

    venue_by_id: dict[int, dict] = {}
    for v in venues:
        vid = v.get("id")
        if vid is not None:
            venue_by_id[int(vid)] = v

    by_norm: dict[str, dict] = {}
    for t in teams:
        # Prefer FBS classification when duplicate names exist
        school = t.get("school") or ""
        key_n = normalize(school)
        prev = by_norm.get(key_n)
        if prev is None or (t.get("classification") == "fbs" and prev.get("classification") != "fbs"):
            by_norm[key_n] = t
        for alt_key in ("alt_name1", "alt_name2", "alt_name3"):
            alt = t.get(alt_key)
            if not alt:
                continue
            ak = normalize(str(alt))
            prev = by_norm.get(ak)
            if prev is None or (t.get("classification") == "fbs" and prev.get("classification") != "fbs"):
                by_norm[ak] = t

    with SCHOOLS_CSV.open(newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))

    out: list[dict[str, object]] = []
    missing: list[str] = []
    for row in rows:
        abbr = row["abbr"]
        csv_name = row["name"]
        if abbr in HARDCODED:
            entry = {"abbr": abbr, **HARDCODED[abbr]}
            out.append(entry)
            continue

        lookup = NAME_OVERRIDES.get(csv_name, csv_name)
        team = by_norm.get(normalize(lookup)) or by_norm.get(normalize(csv_name))
        if team is None:
            missing.append(f"{abbr}:{csv_name}")
            continue

        lat = team.get("location", {}).get("latitude") if isinstance(team.get("location"), dict) else None
        lon = team.get("location", {}).get("longitude") if isinstance(team.get("location"), dict) else None
        city = team.get("location", {}).get("city") if isinstance(team.get("location"), dict) else None
        state = team.get("location", {}).get("state") if isinstance(team.get("location"), dict) else None
        venue_name = team.get("location", {}).get("name") if isinstance(team.get("location"), dict) else None

        vid = team.get("location", {}).get("venue_id") if isinstance(team.get("location"), dict) else None
        if vid is not None and int(vid) in venue_by_id:
            v = venue_by_id[int(vid)]
            if v.get("latitude") is not None:
                lat = v["latitude"]
            if v.get("longitude") is not None:
                lon = v["longitude"]
            city = city or v.get("city")
            state = state or v.get("state")
            venue_name = v.get("name") or venue_name

        if lat is None or lon is None:
            missing.append(f"{abbr}:{csv_name}:no-coords")
            continue

        out.append(
            {
                "abbr": abbr,
                "name": csv_name,
                "lat": float(lat),
                "lon": float(lon),
                "city": city or "",
                "state": state or "",
                "venue": venue_name or "",
            }
        )

    if missing:
        print("WARNING unmatched schools:", ", ".join(missing), file=sys.stderr)
    if len(out) < len(rows) - 5:
        raise SystemExit(f"Too many unmatched schools: {len(out)}/{len(rows)}")
    # Fill remaining missing with HARDCODED or skip — require full coverage
    have = {e["abbr"] for e in out}
    for row in rows:
        if row["abbr"] not in have:
            raise SystemExit(
                f"Unmapped school {row['abbr']} ({row['name']}). Add NAME_OVERRIDES or HARDCODED."
            )
    out.sort(key=lambda e: str(e["abbr"]))
    return out


def ensure_subest() -> Path:
    SUBEST_CACHE.parent.mkdir(parents=True, exist_ok=True)
    if SUBEST_CACHE.exists() and SUBEST_CACHE.stat().st_size > 1_000_000:
        return SUBEST_CACHE
    print(f"Downloading {SUBEST_URL} …")
    req = urllib.request.Request(SUBEST_URL, headers={"User-Agent": "footballcoach-geo/1.0"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        SUBEST_CACHE.write_bytes(resp.read())
    return SUBEST_CACHE


def build_places() -> list[dict[str, object]]:
    # Load gaz lat/lon by GEOIDFQ
    gaz: dict[str, tuple[str, str, float, float]] = {}
    with GAZ.open(encoding="utf-8") as f:
        header = f.readline().strip().split("|")
        idx = {h: i for i, h in enumerate(header)}
        for line in f:
            parts = line.rstrip("\n").split("|")
            geoidfq = parts[idx["GEOIDFQ"]]
            name = parts[idx["NAME"]]
            state = parts[idx["USPS"]]
            lat = float(parts[idx["INTPTLAT"]].strip())
            lon = float(parts[idx["INTPTLONG"]].strip())
            gaz[geoidfq] = (name, state, lat, lon)

    subest = ensure_subest()
    places: list[dict[str, object]] = []
    seen: set[str] = set()
    with subest.open(newline="", encoding="latin-1") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get("SUMLEV") != "162":
                continue
            try:
                pop = int(row["POPESTIMATE2024"])
            except (TypeError, ValueError):
                continue
            if pop < 10000:
                continue
            state = row["STATE"].zfill(2)
            place = row["PLACE"].zfill(5)
            geoidfq = f"1600000US{state}{place}"
            if geoidfq in seen:
                continue
            if geoidfq not in gaz:
                continue
            name, st, lat, lon = gaz[geoidfq]
            display = re.sub(
                r"\s+(city|town|village|CDP|municipality|borough|city and borough)$",
                "",
                name,
                flags=re.I,
            )
            places.append(
                {
                    "geoidfq": geoidfq,
                    "name": display,
                    "state": st,
                    "lat": lat,
                    "lon": lon,
                    "pop": pop,
                }
            )
            seen.add(geoidfq)

    places.sort(key=lambda p: (-int(p["pop"]), str(p["name"])))
    if len(places) < 2000:
        raise SystemExit(f"Expected thousands of places ≥10k, got {len(places)}")
    return places


def main() -> None:
    key = read_cfbd_key()
    print("Fetching CFBD teams/venues…")
    schools = build_schools(key)
    OUT_SCHOOLS.parent.mkdir(parents=True, exist_ok=True)
    OUT_SCHOOLS.write_text(json.dumps({"schools": schools}, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUT_SCHOOLS} ({len(schools)} schools)")

    print("Building places ≥10k from Gaz + ACS…")
    places = build_places()
    OUT_PLACES.write_text(json.dumps({"places": places}, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUT_PLACES} ({len(places)} places)")


if __name__ == "__main__":
    main()
