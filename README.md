# Football Coach 3

**Football Coach 3** is a **modified fork** of [Football Coach](https://github.com/jonesguy14/footballcoach) by **Joachim Jones**. It is **not** the original app and is **not** affiliated with, sponsored by, or endorsed by the original author.

This repository contains substantial changes from the original Work (Compose UI, play-by-play engine, modern roster/positions, 2026 FBS scheduling, offseason systems, and more). See [Attribution & license](#attribution--license) below.

---

## What it is

A college football coach sim: pick a program, manage roster and strategy, play through the season (including play-calling), recruit and handle the offseason, and chase conference and national titles.

You do not control players with a joystick each snap. You coach — schemes, depth chart, recruiting, and (when you want) down-by-down play calls — then simulate.

---

## What’s different in this fork

Relative to the original Football Coach codebase, Football Coach 3 notably includes:

- **Jetpack Compose** UI replacing the legacy Java / XML screens
- A **play-by-play game engine** (concepts, coverages, tempo, box score)
- **2026 FBS conference alignment** and scheduling (bye weeks, conference + OOC)
- Expanded **positions** (e.g. TE, FB, DL, EDGE, LB) and depth-chart / role systems
- Updated **offseason** flow (transfers, NIL-oriented recruiting concepts, prestige-driven budgets)
- Modern Gradle / Kotlin project layout and unit tests around the new sim systems

Saves from the original app (and from older fictional 60-team league formats) are **not** expected to be compatible.

---

## Playing

### Season

- Regular season: **13 weeks** (12 games + 1 locked bye) using the **2026 FBS** map.
- Conferences schedule up to nine conference games; OOC fills open weeks (Pac-12 plays a full round robin; independents are all OOC).
- Before the season, a **Schedule** phase pre-fills a suggested OOC slate you can edit week-by-week (bye is fixed); the CPU fills other teams.
- Use the **Games** tab for schedule, scouting, and game summaries. Rivalry results affect prestige.
- After the regular season: conference championships, bowls, and a national title path for the top teams.
- Prestige rises or falls vs expectations, then you move into the offseason / talent hub.

### Rankings & stats

Poll ranking factors in wins, margin, strength of schedule, and related signals. Team and player stats (yards, scoring, turnovers, etc.) are available from the season UI.

### Roster & coaching

You inherit a full roster with overall/potential and position ratings. Manage depth chart, offensive philosophy / defensive system, and (during games) play calls or auto-sim.

### Recruiting & offseason

After the season, eligibility attrition and transfers open spots. Recruit against a prestige-driven budget; Football Coach 3 also explores NIL/offer-style offseason systems beyond the original recruiting screen.

### Saving

You can save during a season; treat save compatibility as best-effort while the fork is under heavy development. Prefer a fresh league when major format changes land.

---

## Original Football Coach

The original game by Joachim Jones:

- Source: [jonesguy14/footballcoach](https://github.com/jonesguy14/footballcoach)
- Play Store (original app): [Football Coach on Google Play](https://play.google.com/store/apps/details?id=achijones.footballcoach)

This fork’s GitHub remote is [tlebaxter/footballcoach](https://github.com/tlebaxter/footballcoach).

---

## Attribution & license

**Football Coach 3** is a **Derivative Work** based on **Football Coach**.

| | |
| --- | --- |
| **Original title** | Football Coach |
| **Original author** | Joachim Jones |
| **Copyright** | Copyright (c) 2016 Joachim Jones |
| **Upstream** | https://github.com/jonesguy14/footballcoach |
| **This fork** | Football Coach 3 — modified from the original Work |

The original Work and this Derivative Work are distributed under the terms of the Creative Commons license in [`LICENSE.md`](LICENSE.md) (**Attribution–NonCommercial**).

In short (read `LICENSE.md` for the full terms):

1. **Attribution** — Credit Joachim Jones / Football Coach as above; keep copyright and license notices intact.
2. **Modifications** — This fork clearly identifies that the original Work has been modified.
3. **NonCommercial** — You may not use the Work (or this Derivative Work) in a manner primarily intended for commercial advantage or private monetary compensation, unless you obtain separate permission from the copyright holder.
4. **Same license** — Redistribution must remain under this license; you may not add terms that restrict recipients’ rights under it.
5. **No endorsement** — Use of attribution credits does **not** imply endorsement by the original author.

This project is provided **as-is**, without warranty, as described in the license.

---

## Building

Android Studio (recent stable) + JDK matching the project Gradle config. Open the repo root, sync Gradle, and run the `app` module.

```bash
./gradlew :app:installDebug
```

---

## Contributing / status

This is an active personal fork. APIs, save formats, and UI will keep changing. Issues and PRs on [tlebaxter/footballcoach](https://github.com/tlebaxter/footballcoach) are welcome if you are building on this fork under the same license terms.
