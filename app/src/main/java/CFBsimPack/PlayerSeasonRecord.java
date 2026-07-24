package CFBsimPack;

/**
 * One season of a player's career at a school.
 */
public class PlayerSeasonRecord {
    public int seasonYear;
    public String teamAbbr;
    public String teamName;
    public int classYear;
    public int gamesPlayed;
    public int wins;
    public boolean wonHeisman;
    public boolean wonAllAmerican;
    public boolean wonAllConference;
    public String position;

    // Skill stats (0 when unused for position)
    public int passAtt, passComp, passYards, passTd, passInt, sacked;
    public int rushAtt, rushYards, rushTd, rushFumbles;
    public int targets, receptions, recYards, recTd, drops, recFumbles;
    public int xpAtt, xpMade, fgAtt, fgMade;
    public int prAtt, prYards, prTd, krAtt, krYards, krTd, fairCatches;
    public int puntAtt, puntYards;

    public PlayerSeasonRecord() {}

    public PlayerSeasonRecord(Player p, int seasonYear) {
        this.seasonYear = seasonYear;
        this.teamAbbr = p.team != null ? p.team.abbr : "???";
        this.teamName = p.team != null ? p.team.name : "Unknown";
        this.classYear = p.year;
        this.gamesPlayed = p.gamesPlayed;
        this.wins = p.statsWins;
        this.wonHeisman = p.wonHeisman;
        this.wonAllAmerican = p.wonAllAmerican;
        this.wonAllConference = p.wonAllConference;
        this.position = p.position;

        prAtt = p.statsPrAtt;
        prYards = p.statsPrYards;
        prTd = p.statsPrTd;
        krAtt = p.statsKrAtt;
        krYards = p.statsKrYards;
        krTd = p.statsKrTd;
        fairCatches = p.statsFairCatches;

        if (p instanceof PlayerQB) {
            PlayerQB q = (PlayerQB) p;
            passAtt = q.statsPassAtt;
            passComp = q.statsPassComp;
            passYards = q.statsPassYards;
            passTd = q.statsTD;
            passInt = q.statsInt;
            sacked = q.statsSacked;
        } else if (p instanceof PlayerRB) {
            PlayerRB r = (PlayerRB) p;
            rushAtt = r.statsRushAtt;
            rushYards = r.statsRushYards;
            rushTd = r.statsTD;
            rushFumbles = r.statsFumbles;
        } else if (p instanceof PlayerWR) {
            PlayerWR w = (PlayerWR) p;
            targets = w.statsTargets;
            receptions = w.statsReceptions;
            recYards = w.statsRecYards;
            recTd = w.statsTD;
            drops = w.statsDrops;
            recFumbles = w.statsFumbles;
        } else if (p instanceof PlayerK) {
            PlayerK k = (PlayerK) p;
            xpAtt = k.statsXPAtt;
            xpMade = k.statsXPMade;
            fgAtt = k.statsFGAtt;
            fgMade = k.statsFGMade;
        } else if (p instanceof PlayerP) {
            PlayerP punter = (PlayerP) p;
            puntAtt = punter.statsPuntAtt;
            puntYards = punter.statsPuntYards;
        }
    }

    public String classStr() {
        switch (classYear) {
            case 1: return "Fr";
            case 2: return "So";
            case 3: return "Jr";
            case 4: return "Sr";
            case 5: return "Grad";
            default: return "?";
        }
    }

    public String summaryLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(seasonYear).append(" ").append(teamAbbr)
                .append(" [").append(classStr()).append("] ")
                .append(gamesPlayed).append("G (")
                .append(wins).append("-").append(Math.max(0, gamesPlayed - wins)).append(")");
        if ("QB".equals(position)) {
            sb.append("  ").append(passYards).append(" yds, ").append(passTd).append(" TD / ").append(passInt).append(" INT");
        } else if ("RB".equals(position)) {
            sb.append("  ").append(rushYards).append(" yds, ").append(rushTd).append(" TD");
        } else if ("WR".equals(position)) {
            sb.append("  ").append(receptions).append(" rec, ").append(recYards).append(" yds, ").append(recTd).append(" TD");
        } else if ("K".equals(position)) {
            sb.append("  FG ").append(fgMade).append("/").append(fgAtt)
                    .append(", XP ").append(xpMade).append("/").append(xpAtt);
            if (puntAtt > 0) {
                sb.append(", Punt ").append(puntAtt).append("/").append(puntYards);
            }
        }
        if (prAtt > 0 || krAtt > 0) {
            if (prAtt > 0) sb.append("  PR ").append(prYards).append(" yds");
            if (krAtt > 0) sb.append("  KR ").append(krYards).append(" yds");
        }
        return sb.toString();
    }

    /** Compact save token (no commas). */
    public String toSaveToken() {
        return seasonYear + ":" + teamAbbr + ":" + teamName.replace(":", ";") + ":" + classYear + ":"
                + gamesPlayed + ":" + wins + ":"
                + (wonHeisman ? 1 : 0) + (wonAllAmerican ? 1 : 0) + (wonAllConference ? 1 : 0) + ":"
                + position + ":"
                + passAtt + ":" + passComp + ":" + passYards + ":" + passTd + ":" + passInt + ":" + sacked + ":"
                + rushAtt + ":" + rushYards + ":" + rushTd + ":" + rushFumbles + ":"
                + targets + ":" + receptions + ":" + recYards + ":" + recTd + ":" + drops + ":" + recFumbles + ":"
                + xpAtt + ":" + xpMade + ":" + fgAtt + ":" + fgMade + ":"
                + prAtt + ":" + prYards + ":" + prTd + ":" + krAtt + ":" + krYards + ":" + krTd + ":"
                + fairCatches + ":" + puntAtt + ":" + puntYards;
    }

    public static PlayerSeasonRecord fromSaveToken(String token) {
        String[] p = token.split(":");
        if (p.length < 24) return null;
        PlayerSeasonRecord r = new PlayerSeasonRecord();
        try {
            r.seasonYear = Integer.parseInt(p[0]);
            r.teamAbbr = p[1];
            r.teamName = p[2].replace(";", ":");
            r.classYear = Integer.parseInt(p[3]);
            r.gamesPlayed = Integer.parseInt(p[4]);
            r.wins = Integer.parseInt(p[5]);
            String aw = p[6];
            r.wonHeisman = aw.length() > 0 && aw.charAt(0) == '1';
            r.wonAllAmerican = aw.length() > 1 && aw.charAt(1) == '1';
            r.wonAllConference = aw.length() > 2 && aw.charAt(2) == '1';
            r.position = p[7];
            r.passAtt = Integer.parseInt(p[8]);
            r.passComp = Integer.parseInt(p[9]);
            r.passYards = Integer.parseInt(p[10]);
            r.passTd = Integer.parseInt(p[11]);
            r.passInt = Integer.parseInt(p[12]);
            r.sacked = Integer.parseInt(p[13]);
            r.rushAtt = Integer.parseInt(p[14]);
            r.rushYards = Integer.parseInt(p[15]);
            r.rushTd = Integer.parseInt(p[16]);
            r.rushFumbles = Integer.parseInt(p[17]);
            r.targets = Integer.parseInt(p[18]);
            r.receptions = Integer.parseInt(p[19]);
            r.recYards = Integer.parseInt(p[20]);
            r.recTd = Integer.parseInt(p[21]);
            r.drops = Integer.parseInt(p[22]);
            r.recFumbles = Integer.parseInt(p[23]);
            if (p.length > 27) {
                r.xpAtt = Integer.parseInt(p[24]);
                r.xpMade = Integer.parseInt(p[25]);
                r.fgAtt = Integer.parseInt(p[26]);
                r.fgMade = Integer.parseInt(p[27]);
            }
            if (p.length > 36) {
                r.prAtt = Integer.parseInt(p[28]);
                r.prYards = Integer.parseInt(p[29]);
                r.prTd = Integer.parseInt(p[30]);
                r.krAtt = Integer.parseInt(p[31]);
                r.krYards = Integer.parseInt(p[32]);
                r.krTd = Integer.parseInt(p[33]);
                r.fairCatches = Integer.parseInt(p[34]);
                r.puntAtt = Integer.parseInt(p[35]);
                r.puntYards = Integer.parseInt(p[36]);
            }
        } catch (Exception e) {
            return null;
        }
        return r;
    }
}
