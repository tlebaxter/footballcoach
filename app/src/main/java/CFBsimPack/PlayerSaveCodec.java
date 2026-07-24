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
        if (p instanceof PlayerQB) {
            PlayerQB q = (PlayerQB) p;
            sb.append(',').append(q.careerPassAtt).append(',').append(q.careerPassComp)
                    .append(',').append(q.careerTDs).append(',').append(q.careerInt)
                    .append(',').append(q.careerPassYards).append(',').append(q.careerSacked)
                    .append(',').append(q.careerRushAtt).append(',').append(q.careerRushYards)
                    .append(',').append(q.careerRushTD);
        } else if (p instanceof PlayerRB) {
            PlayerRB r = (PlayerRB) p;
            sb.append(',').append(r.careerRushAtt).append(',').append(r.careerRushYards)
                    .append(',').append(r.careerTDs).append(',').append(r.careerFumbles);
        } else if (p instanceof PlayerWR) {
            PlayerWR w = (PlayerWR) p;
            sb.append(',').append(w.careerTargets).append(',').append(w.careerReceptions)
                    .append(',').append(w.careerRecYards).append(',').append(w.careerTDs)
                    .append(',').append(w.careerDrops).append(',').append(w.careerFumbles);
        } else if (p instanceof PlayerTE) {
            PlayerTE t = (PlayerTE) p;
            sb.append(',').append(t.careerTargets).append(',').append(t.careerReceptions)
                    .append(',').append(t.careerRecYards).append(',').append(t.careerTDs);
        } else if (p instanceof PlayerFB) {
            PlayerFB fb = (PlayerFB) p;
            sb.append(',').append(fb.careerRushAtt).append(',').append(fb.careerRushYards)
                    .append(',').append(fb.careerTDs);
        } else if (p instanceof PlayerK) {
            PlayerK k = (PlayerK) p;
            sb.append(',').append(k.careerXPAtt).append(',').append(k.careerXPMade)
                    .append(',').append(k.careerFGAtt).append(',').append(k.careerFGMade);
        } else if (p instanceof PlayerP) {
            PlayerP punter = (PlayerP) p;
            sb.append(',').append(punter.careerPuntAtt).append(',').append(punter.careerPuntYards);
        }
        sb.append(',').append(p.careerHeismans).append(',').append(p.careerAllAmerican)
                .append(',').append(p.careerAllConference).append(',').append(p.careerWins);
        return sb.toString();
    }

    private static void loadCareer(Player p, String[] f, int idx) {
        p.careerGamesPlayed = parse(f, idx++, 0);
        if (p instanceof PlayerQB) {
            PlayerQB q = (PlayerQB) p;
            q.careerPassAtt = parse(f, idx++, 0);
            q.careerPassComp = parse(f, idx++, 0);
            q.careerTDs = parse(f, idx++, 0);
            q.careerInt = parse(f, idx++, 0);
            q.careerPassYards = parse(f, idx++, 0);
            q.careerSacked = parse(f, idx++, 0);
            q.careerRushAtt = parse(f, idx++, 0);
            q.careerRushYards = parse(f, idx++, 0);
            q.careerRushTD = parse(f, idx++, 0);
        } else if (p instanceof PlayerRB) {
            PlayerRB r = (PlayerRB) p;
            r.careerRushAtt = parse(f, idx++, 0);
            r.careerRushYards = parse(f, idx++, 0);
            r.careerTDs = parse(f, idx++, 0);
            r.careerFumbles = parse(f, idx++, 0);
        } else if (p instanceof PlayerWR) {
            PlayerWR w = (PlayerWR) p;
            w.careerTargets = parse(f, idx++, 0);
            w.careerReceptions = parse(f, idx++, 0);
            w.careerRecYards = parse(f, idx++, 0);
            w.careerTDs = parse(f, idx++, 0);
            w.careerDrops = parse(f, idx++, 0);
            w.careerFumbles = parse(f, idx++, 0);
        } else if (p instanceof PlayerTE) {
            PlayerTE t = (PlayerTE) p;
            t.careerTargets = parse(f, idx++, 0);
            t.careerReceptions = parse(f, idx++, 0);
            t.careerRecYards = parse(f, idx++, 0);
            t.careerTDs = parse(f, idx++, 0);
        } else if (p instanceof PlayerFB) {
            PlayerFB fb = (PlayerFB) p;
            fb.careerRushAtt = parse(f, idx++, 0);
            fb.careerRushYards = parse(f, idx++, 0);
            fb.careerTDs = parse(f, idx++, 0);
        } else if (p instanceof PlayerK) {
            PlayerK k = (PlayerK) p;
            k.careerXPAtt = parse(f, idx++, 0);
            k.careerXPMade = parse(f, idx++, 0);
            k.careerFGAtt = parse(f, idx++, 0);
            k.careerFGMade = parse(f, idx++, 0);
        } else if (p instanceof PlayerP) {
            PlayerP punter = (PlayerP) p;
            punter.careerPuntAtt = parse(f, idx++, 0);
            punter.careerPuntYards = parse(f, idx++, 0);
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
