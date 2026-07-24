package CFBsimPack;

import java.util.Random;

/**
 * Builds players with a full {@link PlayerRatings} bag from stars / archetypes.
 */
public final class PlayerFactory {

    private PlayerFactory() {}

    public static Player fromStars(PositionGroup pos, String name, int year, int stars, Team team, Random rng) {
        Random r = rng != null ? rng : new Random();
        int y = Math.max(1, Math.min(5, year));
        int s = Math.max(1, Math.min(5, stars));
        PlayerRatings ratings = rollRatings(pos, y, s, r);
        Player p = createShell(pos, name, team, y);
        p.applyRatings(ratings);
        p.isRedshirt = false;
        p.gamesPlayed = 0;
        p.isInjured = false;
        p.recomputeCost(r);
        return p;
    }

    public static Player fromRatings(
            PositionGroup pos, String name, Team team, int year, PlayerRatings ratings, boolean redshirt) {
        Player p = createShell(pos, name, team, year);
        p.applyRatings(ratings != null ? ratings : new PlayerRatings());
        p.isRedshirt = redshirt;
        p.gamesPlayed = 0;
        p.isInjured = false;
        p.recomputeCost(new Random(name != null ? name.hashCode() : 0));
        return p;
    }

    public static PlayerRatings rollRatings(PositionGroup pos, int year, int stars, Random rng) {
        Random r = rng != null ? rng : new Random();
        PlayerRatings out = new PlayerRatings();
        out.pot = clampRoll(50 + (int) (50 * r.nextDouble()), r);
        out.footIq = clampRoll(50 + (int) (50 * r.nextDouble()), r);
        out.dur = clampRoll(50 + (int) (50 * r.nextDouble()), r);

        int base = 55 + year * 4 + stars * 5;
        // Athletic floor for everyone
        out.hgt = skill(base - 5, r);
        out.stre = skill(base - 5, r);
        out.spd = skill(base - 5, r);
        out.endu = skill(base - 8, r);
        out.bsc = skill(base - 10, r);
        out.elu = skill(base - 10, r);
        out.rtr = skill(base - 12, r);
        out.hnd = skill(base - 12, r);
        out.pbk = skill(base - 15, r);
        out.rbk = skill(base - 15, r);
        out.pcv = skill(base - 15, r);
        out.tck = skill(base - 12, r);
        out.prs = skill(base - 15, r);
        out.rns = skill(base - 15, r);
        out.thv = skill(base - 20, r);
        out.thp = skill(base - 20, r);
        out.tha = skill(base - 20, r);
        out.kpw = skill(base - 20, r);
        out.kac = skill(base - 20, r);
        out.ppw = skill(base - 20, r);
        out.pac = skill(base - 20, r);

        applyArchetypeBoosts(out, pos, base, r);
        return out;
    }

    private static void applyArchetypeBoosts(PlayerRatings out, PositionGroup pos, int base, Random r) {
        switch (pos) {
            case QB: {
                boolean dual = r.nextDouble() < 0.35;
                boost(out, "tha", 12, r);
                boost(out, "thp", 10, r);
                boost(out, "thv", 10, r);
                boost(out, "bsc", 6, r);
                if (dual) {
                    boost(out, "spd", 10, r);
                    boost(out, "elu", 10, r);
                } else {
                    boost(out, "tha", 4, r);
                    boost(out, "thv", 4, r);
                }
                break;
            }
            case RB: {
                boolean power = r.nextDouble() < 0.4;
                boost(out, "spd", 10, r);
                boost(out, "elu", 10, r);
                boost(out, "bsc", 8, r);
                boost(out, "hnd", 4, r);
                if (power) boost(out, "stre", 10, r);
                else boost(out, "spd", 4, r);
                break;
            }
            case FB:
                boost(out, "rbk", 14, r);
                boost(out, "pbk", 8, r);
                boost(out, "stre", 10, r);
                boost(out, "bsc", 6, r);
                boost(out, "hnd", 4, r);
                break;
            case WR:
                boost(out, "hnd", 12, r);
                boost(out, "rtr", 12, r);
                boost(out, "spd", 10, r);
                boost(out, "hgt", 6, r);
                break;
            case TE:
                boost(out, "hnd", 8, r);
                boost(out, "rbk", 8, r);
                boost(out, "pbk", 6, r);
                boost(out, "hgt", 8, r);
                boost(out, "rtr", 6, r);
                break;
            case OL:
                boost(out, "pbk", 12, r);
                boost(out, "rbk", 12, r);
                boost(out, "stre", 10, r);
                boost(out, "hgt", 8, r);
                break;
            case EDGE:
                boost(out, "prs", 14, r);
                boost(out, "spd", 8, r);
                boost(out, "stre", 8, r);
                boost(out, "tck", 6, r);
                boost(out, "rns", 4, r);
                break;
            case DL:
                boost(out, "rns", 12, r);
                boost(out, "prs", 8, r);
                boost(out, "stre", 12, r);
                boost(out, "tck", 6, r);
                break;
            case LB:
                boost(out, "tck", 10, r);
                boost(out, "rns", 8, r);
                boost(out, "pcv", 6, r);
                boost(out, "prs", 4, r);
                boost(out, "spd", 4, r);
                break;
            case CB:
                boost(out, "pcv", 14, r);
                boost(out, "spd", 12, r);
                boost(out, "tck", 4, r);
                break;
            case S:
                boost(out, "pcv", 10, r);
                boost(out, "tck", 8, r);
                boost(out, "spd", 8, r);
                break;
            case K:
                boost(out, "kpw", 16, r);
                boost(out, "kac", 16, r);
                break;
            case P:
                boost(out, "ppw", 16, r);
                boost(out, "pac", 16, r);
                break;
            default:
                break;
        }
        // Mild noise on primary skills already applied via boost
        out.hgt = PlayerRatings.clamp(out.hgt);
    }

    private static void boost(PlayerRatings out, String key, int amount, Random r) {
        int jitter = (int) (r.nextDouble() * 8) - 3;
        out.bump(key, amount + jitter);
    }

    private static int skill(int base, Random r) {
        return clampRoll(base - (int) (22 * r.nextDouble()), r);
    }

    private static int clampRoll(int v, Random r) {
        return PlayerRatings.clamp(v);
    }

    private static Player createShell(PositionGroup pos, String name, Team team, int year) {
        Player p;
        switch (pos) {
            case QB: p = new PlayerQB(); break;
            case RB: p = new PlayerRB(); break;
            case FB: p = new PlayerFB(); break;
            case WR: p = new PlayerWR(); break;
            case TE: p = new PlayerTE(); break;
            case OL: p = new PlayerOL(); break;
            case EDGE: p = new PlayerEDGE(); break;
            case DL: p = new PlayerDL(); break;
            case LB: p = new PlayerLB(); break;
            case CB: p = new PlayerCB(); break;
            case S: p = new PlayerS(); break;
            case K: p = new PlayerK(); break;
            case P: p = new PlayerP(); break;
            default: p = new PlayerLB(); break;
        }
        p.name = name;
        p.team = team;
        p.year = year;
        p.position = pos.token;
        return p;
    }
}
