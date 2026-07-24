#!/usr/bin/env python3
"""Generate numeric program profiles from categorical, reviewable seed tiers."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/fbs_2026_classes.csv"
OUTPUT = ROOT / "app/src/main/assets/fbs_2026.csv"

PROGRAM_CLASSES: dict[str, tuple[int, int, int]] = {
    # tradition, fanbase, donors
    "BLUE_BLOOD": (96, 96, 96),
    "NATIONAL_BRAND": (90, 91, 90),
    "STRONG_POWER": (83, 85, 84),
    "SOLID_POWER": (76, 78, 76),
    "RISING_POWER": (68, 74, 73),
    "ESTABLISHED_G5": (65, 70, 64),
    "MID": (57, 60, 56),
    "DEVELOPING": (44, 48, 45),
}

REGION_FOOTPRINT: dict[str, int] = {
    "FL": 96,
    "TX": 94,
    "SE": 92,
    "CA": 91,
    "MIDWEST": 78,
    "PLAINS": 73,
    "PACIFIC": 72,
    "MOUNTAIN": 66,
    "NORTHEAST": 60,
}

PIPELINE_CLASSES: dict[str, int] = {
    "ELITE_FACTORY": 95,
    "STRONG_FACTORY": 86,
    "PRODUCER": 75,
    "OCCASIONAL": 63,
    "LIMITED": 49,
}

MOMENTUM_CLASSES: dict[str, int] = {
    "CHAMPION": 99,
    "ELITE": 94,
    "CONTENDER": 88,
    "STRONG": 80,
    "AVERAGE": 62,
    "REBUILD": 50,
}

# Final 2025 AP poll (January 2026) plus teams receiving votes. Momentum is
# intentionally independent from historic program class.
CHAMPION = {"IND"}
ELITE = {"MIA", "OLM", "ORE", "OSU", "UGA", "TTU", "TAM", "ALA", "NDE"}
CONTENDER = {
    "BYU", "TEX", "OKL", "UTA", "VAN", "UVA", "IOW", "TUL", "JMU",
    "USC", "MIC", "HOU", "NAV", "UNT", "TCU",
}
STRONG = {
    "ILL", "WAS", "SMU", "DUK", "ARI", "GAT", "TEN", "MIZ", "LOU",
    "WMU", "WFU", "HAW", "BOI",
}

FIELDS = (
    "conference",
    "name",
    "abbr",
    "tradition",
    "fanbase",
    "donors",
    "footprint",
    "pipeline",
    "momentum",
    "rivals",
)


def deterministic_jitter(abbreviation: str, factor: str) -> int:
    digest = hashlib.sha256(f"{abbreviation}:{factor}".encode()).digest()
    return digest[0] % 5 - 2


def bounded(value: int) -> int:
    return max(25, min(99, value))


def momentum_class(abbreviation: str, program_class: str) -> str:
    if abbreviation in CHAMPION:
        return "CHAMPION"
    if abbreviation in ELITE:
        return "ELITE"
    if abbreviation in CONTENDER:
        return "CONTENDER"
    if abbreviation in STRONG:
        return "STRONG"
    if program_class in {"MID", "DEVELOPING"}:
        return "REBUILD"
    return "AVERAGE"


def generate_row(row: dict[str, str]) -> dict[str, str | int]:
    program_class = row["programClass"]
    region = row["region"]
    pipeline_class = row["pipelineClass"]
    try:
        tradition, fanbase, donors = PROGRAM_CLASSES[program_class]
        footprint = REGION_FOOTPRINT[region]
        pipeline = PIPELINE_CLASSES[pipeline_class]
        momentum = MOMENTUM_CLASSES[momentum_class(row["abbr"], program_class)]
    except KeyError as error:
        raise ValueError(
            f"Unknown seed class {error.args[0]!r} for {row.get('abbr', '?')}"
        ) from error

    abbreviation = row["abbr"]
    numeric = {
        "tradition": tradition,
        "fanbase": fanbase,
        "donors": donors,
        "footprint": footprint,
        "pipeline": pipeline,
        "momentum": momentum,
    }
    for factor, value in numeric.items():
        numeric[factor] = bounded(value + deterministic_jitter(abbreviation, factor))

    return {
        "conference": row["conference"],
        "name": row["name"],
        "abbr": abbreviation,
        **numeric,
        "rivals": row["rivals"],
    }


def main() -> None:
    with SOURCE.open(newline="", encoding="utf-8") as source:
        rows = list(csv.DictReader(source))
    if len(rows) != 140:
        raise ValueError(f"Expected 140 class rows, found {len(rows)}")

    abbreviations = [row["abbr"] for row in rows]
    if len(abbreviations) != len(set(abbreviations)):
        raise ValueError("Duplicate team abbreviation in program seed classes")

    generated = [generate_row(row) for row in rows]
    with OUTPUT.open("w", newline="", encoding="utf-8") as output:
        writer = csv.DictWriter(output, fieldnames=FIELDS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(generated)


if __name__ == "__main__":
    main()
