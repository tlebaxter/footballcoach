package CFBsimPack;

/**
 * SAVE_VERSION 6 player CSV:
 * pos,name,year,pot,iq,dur,{21 attrs},ovr,improvement,career...,extras
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
        sb.append(p.careerGamesPlayed);
        PlayerSkillStats c = p.careerStats;
        PositionGroup g = PositionGroup.fromToken(p.position);
        if (g == PositionGroup.QB) {
            sb.append(',').append(c.passAtt).append(',').append(c.passComp)
                    .append(',').append(c.passTd).append(',').append(c.passInt)
                    .append(',').append(c.passYards).append(',').append(c.sacked)
                    .append(',').append(c.rushAtt).append(',').append(c.rushYards)
                    .append(',').append(c.rushTd);
        } else if (g == PositionGroup.RB) {
            sb.append(',').append(c.rushAtt).append(',').append(c.rushYards)
                    .append(',').append(c.rushTd).append(',').append(c.fumbles);
        } else if (g == PositionGroup.WR) {
            sb.append(',').append(c.targets).append(',').append(c.receptions)
                    .append(',').append(c.recYards).append(',').append(c.recTd)
                    .append(',').append(c.drops).append(',').append(c.recFumbles);
        } else if (g == PositionGroup.TE) {
            sb.append(',').append(c.targets).append(',').append(c.receptions)
                    .append(',').append(c.recYards).append(',').append(c.recTd);
        } else if (g == PositionGroup.FB) {
            sb.append(',').append(c.rushAtt).append(',').append(c.rushYards)
                    .append(',').append(c.rushTd);
        } else if (g == PositionGroup.K) {
            sb.append(',').append(c.xpAtt).append(',').append(c.xpMade)
                    .append(',').append(c.fgAtt).append(',').append(c.fgMade);
        } else if (g == PositionGroup.P) {
            sb.append(',').append(c.puntAtt).append(',').append(c.puntYards);
        }
        sb.append(',').append(p.careerHeismans).append(',').append(p.careerAllAmerican)
                .append(',').append(p.careerAllConference).append(',').append(p.careerWins);
        return sb.toString();
    }

    private static void loadCareer(Player p, String[] f, int idx) {
        p.careerGamesPlayed = parse(f, idx++, 0);
        PlayerSkillStats c = p.careerStats;
        PositionGroup g = PositionGroup.fromToken(p.position);
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
        p.careerHeismans = parse(f, idx++, 0);
        p.careerAllAmerican = parse(f, idx++, 0);
        p.careerAllConference = parse(f, idx++, 0);
        p.careerWins = parse(f, idx, 0);
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
