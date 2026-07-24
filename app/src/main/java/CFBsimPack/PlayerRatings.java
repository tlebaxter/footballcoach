package CFBsimPack;

import java.util.Locale;

/**
 * Shared descriptive rating bag (0–100) for every player.
 * Meta: pot, footIq, dur. Skills mirror ZenGM-style coverage.
 */
public final class PlayerRatings {

    public static final String[] KEYS = {
            "hgt", "stre", "spd", "endu",
            "thv", "thp", "tha",
            "bsc", "elu", "rtr", "hnd",
            "pbk", "rbk",
            "pcv", "tck", "prs", "rns",
            "kpw", "kac", "ppw", "pac"
    };

    public int pot;
    public int footIq;
    public int dur;

    public int hgt;
    public int stre;
    public int spd;
    public int endu;
    public int thv;
    public int thp;
    public int tha;
    public int bsc;
    public int elu;
    public int rtr;
    public int hnd;
    public int pbk;
    public int rbk;
    public int pcv;
    public int tck;
    public int prs;
    public int rns;
    public int kpw;
    public int kac;
    public int ppw;
    public int pac;

    public PlayerRatings() {
        pot = 50;
        footIq = 50;
        dur = 50;
        for (String k : KEYS) {
            set(k, 50);
        }
    }

    public PlayerRatings copy() {
        PlayerRatings c = new PlayerRatings();
        c.pot = pot;
        c.footIq = footIq;
        c.dur = dur;
        for (String k : KEYS) {
            c.set(k, get(k));
        }
        return c;
    }

    public int get(String key) {
        switch (key) {
            case "hgt": return hgt;
            case "stre": return stre;
            case "spd": return spd;
            case "endu": return endu;
            case "thv": return thv;
            case "thp": return thp;
            case "tha": return tha;
            case "bsc": return bsc;
            case "elu": return elu;
            case "rtr": return rtr;
            case "hnd": return hnd;
            case "pbk": return pbk;
            case "rbk": return rbk;
            case "pcv": return pcv;
            case "tck": return tck;
            case "prs": return prs;
            case "rns": return rns;
            case "kpw": return kpw;
            case "kac": return kac;
            case "ppw": return ppw;
            case "pac": return pac;
            case "pot": return pot;
            case "footIq": return footIq;
            case "dur": return dur;
            default: throw new IllegalArgumentException("Unknown rating key: " + key);
        }
    }

    public void set(String key, int value) {
        int v = clamp(value);
        switch (key) {
            case "hgt": hgt = v; break;
            case "stre": stre = v; break;
            case "spd": spd = v; break;
            case "endu": endu = v; break;
            case "thv": thv = v; break;
            case "thp": thp = v; break;
            case "tha": tha = v; break;
            case "bsc": bsc = v; break;
            case "elu": elu = v; break;
            case "rtr": rtr = v; break;
            case "hnd": hnd = v; break;
            case "pbk": pbk = v; break;
            case "rbk": rbk = v; break;
            case "pcv": pcv = v; break;
            case "tck": tck = v; break;
            case "prs": prs = v; break;
            case "rns": rns = v; break;
            case "kpw": kpw = v; break;
            case "kac": kac = v; break;
            case "ppw": ppw = v; break;
            case "pac": pac = v; break;
            case "pot": pot = v; break;
            case "footIq": footIq = v; break;
            case "dur": dur = v; break;
            default: throw new IllegalArgumentException("Unknown rating key: " + key);
        }
    }

    public void bump(String key, int delta) {
        set(key, get(key) + delta);
    }

    /** CSV segment: pot,footIq,dur, then KEYS in order. */
    public String toCsvSegment() {
        StringBuilder sb = new StringBuilder();
        sb.append(pot).append(',').append(footIq).append(',').append(dur);
        for (String k : KEYS) {
            sb.append(',').append(get(k));
        }
        return sb.toString();
    }

    /** Parse from fields starting at index {@code start} (pot). Returns next index after ratings. */
    public int fromCsvFields(String[] fields, int start) {
        pot = parse(fields, start);
        footIq = parse(fields, start + 1);
        dur = parse(fields, start + 2);
        int i = start + 3;
        for (String k : KEYS) {
            set(k, parse(fields, i++));
        }
        return i;
    }

    public static int csvWidth() {
        return 3 + KEYS.length;
    }

    public static String displayLabel(String key) {
        switch (key) {
            case "hgt": return "Hgt";
            case "stre": return "Str";
            case "spd": return "Spd";
            case "endu": return "End";
            case "thv": return "ThV";
            case "thp": return "ThP";
            case "tha": return "ThA";
            case "bsc": return "BSc";
            case "elu": return "Elu";
            case "rtr": return "RtR";
            case "hnd": return "Hnd";
            case "pbk": return "PBk";
            case "rbk": return "RBk";
            case "pcv": return "PCv";
            case "tck": return "Tck";
            case "prs": return "PRs";
            case "rns": return "RnS";
            case "kpw": return "KPw";
            case "kac": return "KAc";
            case "ppw": return "PPw";
            case "pac": return "PAc";
            case "pot": return "Pot";
            case "footIq": return "IQ";
            case "dur": return "Dur";
            default: return key.toUpperCase(Locale.US);
        }
    }

    private static int parse(String[] fields, int i) {
        if (i < 0 || i >= fields.length) return 50;
        try {
            return clamp(Integer.parseInt(fields[i].trim()));
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    public static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }
}
