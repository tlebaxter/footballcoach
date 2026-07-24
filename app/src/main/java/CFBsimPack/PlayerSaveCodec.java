package CFBsimPack;

/**
 * Player CSV codec.
 * SAVE_VERSION 6+: pos,name,year,pot,iq,dur,{21 attrs},ovr,improvement,career...,extras
 * SAVE_VERSION 7+: optional {@code |SEASON,...} suffix with live season counters + injury.
 * SAVE_VERSION 8+: season snaps after wins; careerSnaps after careerGamesPlayed.
 */
public final class PlayerSaveCodec {

    private PlayerSaveCodec() {}

    public static String toLine(Player p) {
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(p.position).append(',').append(p.name).append(',').append(p.year).append(',');
        sb.append(p.ratingsSaveCsv());
        sb.append(',').append(careerCsv(p));
        return sb.toString();
    }

    /** Optional mid-season progress suffix (empty when nothing to persist). */
    public static String seasonSuffix(Player p) {
        if (p == null) return "";
        boolean hasSeason = p.gamesPlayed > 0 || p.statsWins > 0 || p.seasonSnaps > 0
                || p.isInjured || p.injury != null || p.isEjected
                || hasSkill(p.seasonStats);
        if (!hasSeason) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("|SEASON,").append(p.gamesPlayed).append(',').append(p.statsWins)
                .append(',').append(p.seasonSnaps);
        String skills = skillCsv(p.seasonStats, PositionGroup.fromToken(p.position));
        if (!skills.isEmpty()) {
            sb.append(',').append(skills);
        }
        sb.append(',').append(injuryToken(p));
        sb.append(',').append(p.isEjected ? 1 : 0);
        return sb.toString();
    }

    public static void loadSeasonFromSuffix(Player p, String suffix) {
        if (p == null || suffix == null || !suffix.startsWith("|SEASON")) return;
        String body = suffix.startsWith("|SEASON,") ? suffix.substring("|SEASON,".length()) : "";
        if (body.isEmpty()) return;
        String[] f = body.split(",", -1);
        int idx = 0;
        p.gamesPlayed = parse(f, idx++, 0);
        p.statsWins = parse(f, idx++, 0);
        PositionGroup g = PositionGroup.fromToken(p.position);
        int skillCount = skillFieldCount(g);
        // v8: gp,wins,snaps,skills...,injury — v7: gp,wins,skills...,injury
        int remaining = f.length - idx;
        if (remaining >= skillCount + 2) {
            p.seasonSnaps = parse(f, idx++, 0);
        }
        idx = loadSkill(p.seasonStats, g, f, idx);
        applyInjuryToken(p, idx < f.length ? f[idx] : "none");
        idx++;
        p.isEjected = idx < f.length && parse(f, idx, 0) != 0;
    }

    public static Player fromFields(Team team, String[] f, boolean redshirt) {
        if (f == null || f.length < 3) return null;
        PositionGroup pos = PositionGroup.fromToken(f[0]);
        if (pos == null) return null;
        String name = f[1];
        int year = parse(f, 2, 1);
        PlayerRatings bag = new PlayerRatings();
        int idx = bag.fromCsvFields(f, 3);
        // ovr, improvement
        int ovr = parse(f, idx, 50);
        int improve = parse(f, idx + 1, 0);
        idx += 2;
        Player p = PlayerFactory.fromRatings(pos, name, team, year, bag, redshirt);
        p.ratImprovement = improve;
        // keep computed ovr from bag; ignore saved ovr unless bag empty
        if (p.ratOvr <= 0) p.ratOvr = ovr;
        loadCareer(p, f, idx);
        return p;
    }

    private static String careerCsv(Player p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.careerGamesPlayed).append(',').append(p.careerSnaps);
        String skills = skillCsv(p.careerStats, PositionGroup.fromToken(p.position));
        if (!skills.isEmpty()) {
            sb.append(',').append(skills);
        }
        sb.append(',').append(p.careerHeismans).append(',').append(p.careerAllAmerican)
                .append(',').append(p.careerAllConference).append(',').append(p.careerWins);
        return sb.toString();
    }

    /** Position skill counters CSV, or empty for positions without skill bags in the save format. */
    private static String skillCsv(PlayerSkillStats c, PositionGroup g) {
        if (g == PositionGroup.QB) {
            return c.passAtt + "," + c.passComp + "," + c.passTd + "," + c.passInt + ","
                    + c.passYards + "," + c.sacked + "," + c.rushAtt + "," + c.rushYards + ","
                    + c.rushTd;
        } else if (g == PositionGroup.RB) {
            return c.rushAtt + "," + c.rushYards + "," + c.rushTd + "," + c.fumbles;
        } else if (g == PositionGroup.WR) {
            return c.targets + "," + c.receptions + "," + c.recYards + "," + c.recTd + ","
                    + c.drops + "," + c.recFumbles;
        } else if (g == PositionGroup.TE) {
            return c.targets + "," + c.receptions + "," + c.recYards + "," + c.recTd;
        } else if (g == PositionGroup.FB) {
            return c.rushAtt + "," + c.rushYards + "," + c.rushTd;
        } else if (g == PositionGroup.K) {
            return c.xpAtt + "," + c.xpMade + "," + c.fgAtt + "," + c.fgMade;
        } else if (g == PositionGroup.P) {
            return c.puntAtt + "," + c.puntYards;
        }
        return "";
    }

    private static void loadCareer(Player p, String[] f, int idx) {
        p.careerGamesPlayed = parse(f, idx++, 0);
        PlayerSkillStats c = p.careerStats;
        PositionGroup g = PositionGroup.fromToken(p.position);
        int skillCount = skillFieldCount(g);
        // v8: games,snaps,skills,awards(+roster) — v7: games,skills,awards(+roster)
        int remaining = f.length - idx;
        if (remaining >= skillCount + 6) {
            p.careerSnaps = parse(f, idx++, 0);
        }
        idx = loadSkill(c, g, f, idx);
        p.careerHeismans = parse(f, idx++, 0);
        p.careerAllAmerican = parse(f, idx++, 0);
        p.careerAllConference = parse(f, idx++, 0);
        p.careerWins = parse(f, idx, 0);
    }

    private static int skillFieldCount(PositionGroup g) {
        if (g == PositionGroup.QB) return 9;
        if (g == PositionGroup.RB) return 4;
        if (g == PositionGroup.WR) return 6;
        if (g == PositionGroup.TE) return 4;
        if (g == PositionGroup.FB) return 3;
        if (g == PositionGroup.K) return 4;
        if (g == PositionGroup.P) return 2;
        return 0;
    }

    private static int loadSkill(PlayerSkillStats c, PositionGroup g, String[] f, int idx) {
        if (g == PositionGroup.QB) {
            c.passAtt = parse(f, idx++, 0);
            c.passComp = parse(f, idx++, 0);
            c.passTd = parse(f, idx++, 0);
            c.passInt = parse(f, idx++, 0);
            c.passYards = parse(f, idx++, 0);
            c.sacked = parse(f, idx++, 0);
            c.rushAtt = parse(f, idx++, 0);
            c.rushYards = parse(f, idx++, 0);
            c.rushTd = parse(f, idx++, 0);
        } else if (g == PositionGroup.RB) {
            c.rushAtt = parse(f, idx++, 0);
            c.rushYards = parse(f, idx++, 0);
            c.rushTd = parse(f, idx++, 0);
            c.fumbles = parse(f, idx++, 0);
        } else if (g == PositionGroup.WR) {
            c.targets = parse(f, idx++, 0);
            c.receptions = parse(f, idx++, 0);
            c.recYards = parse(f, idx++, 0);
            c.recTd = parse(f, idx++, 0);
            c.drops = parse(f, idx++, 0);
            c.recFumbles = parse(f, idx++, 0);
        } else if (g == PositionGroup.TE) {
            c.targets = parse(f, idx++, 0);
            c.receptions = parse(f, idx++, 0);
            c.recYards = parse(f, idx++, 0);
            c.recTd = parse(f, idx++, 0);
        } else if (g == PositionGroup.FB) {
            c.rushAtt = parse(f, idx++, 0);
            c.rushYards = parse(f, idx++, 0);
            c.rushTd = parse(f, idx++, 0);
        } else if (g == PositionGroup.K) {
            c.xpAtt = parse(f, idx++, 0);
            c.xpMade = parse(f, idx++, 0);
            c.fgAtt = parse(f, idx++, 0);
            c.fgMade = parse(f, idx++, 0);
        } else if (g == PositionGroup.P) {
            c.puntAtt = parse(f, idx++, 0);
            c.puntYards = parse(f, idx++, 0);
        }
        return idx;
    }

    private static boolean hasSkill(PlayerSkillStats s) {
        if (s == null) return false;
        return s.passAtt != 0 || s.passComp != 0 || s.passYards != 0 || s.passTd != 0 || s.passInt != 0
                || s.sacked != 0 || s.rushAtt != 0 || s.rushYards != 0 || s.rushTd != 0 || s.fumbles != 0
                || s.targets != 0 || s.receptions != 0 || s.recYards != 0 || s.recTd != 0 || s.drops != 0
                || s.recFumbles != 0 || s.xpAtt != 0 || s.xpMade != 0 || s.fgAtt != 0 || s.fgMade != 0
                || s.puntAtt != 0 || s.puntYards != 0;
    }

    private static String injuryToken(Player p) {
        if (p.injury == null || !p.isInjured) return "none";
        String desc = p.injury.getDescription();
        if (desc == null || desc.isEmpty()) desc = "Injury";
        return desc.replace(',', '_').replace('|', '_') + ":" + p.injury.getDuration();
    }

    private static void applyInjuryToken(Player p, String token) {
        if (token == null || token.isEmpty() || "none".equals(token)) {
            p.isInjured = false;
            p.injury = null;
            return;
        }
        int colon = token.lastIndexOf(':');
        if (colon <= 0 || colon >= token.length() - 1) {
            p.isInjured = false;
            p.injury = null;
            return;
        }
        String desc = token.substring(0, colon).replace('_', ' ');
        int dur;
        try {
            dur = Integer.parseInt(token.substring(colon + 1).trim());
        } catch (Exception e) {
            dur = 1;
        }
        if (dur < 1) dur = 1;
        p.injury = new Injury(dur, desc, p);
    }

    private static int parse(String[] f, int i, int def) {
        if (i < 0 || i >= f.length) return def;
        try {
            return Integer.parseInt(f[i].trim());
        } catch (Exception e) {
            return def;
        }
    }
}
