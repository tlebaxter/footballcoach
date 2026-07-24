package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One rivalry link from a team to an opponent abbreviation.
 * Strength is 0–100 and evolves over seasons.
 */
public final class Rivalry {
    public static final int MAX_RIVALRIES = 5;
    public static final int SEAT_THRESHOLD = 50;
    public static final int MOMENTUM_THRESHOLD = 40;
    public static final int HOT_THRESHOLD = 70;
    public static final int WARM_THRESHOLD = 40;

    public final String opponentAbbr;
    public int strength;

    public Rivalry(String opponentAbbr, int strength) {
        if (opponentAbbr == null || opponentAbbr.trim().isEmpty()) {
            throw new IllegalArgumentException("Rival opponent abbreviation is required.");
        }
        this.opponentAbbr = opponentAbbr.trim().toUpperCase();
        this.strength = clamp(strength);
    }

    public static int clamp(int strength) {
        if (strength < 0) {
            return 0;
        }
        if (strength > 100) {
            return 100;
        }
        return strength;
    }

    /** Display band for UI: Hot / Warm / Cold. */
    public static String band(int strength) {
        if (strength >= HOT_THRESHOLD) {
            return "Hot";
        }
        if (strength >= WARM_THRESHOLD) {
            return "Warm";
        }
        if (strength > 0) {
            return "Cold";
        }
        return "None";
    }

    public String band() {
        return band(strength);
    }

    public String displayLabel() {
        return opponentAbbr + " " + strength + " · " + band();
    }

    /**
     * Parse strength token: numeric, or legacy P/S/T codes.
     */
    public static int parseStrengthToken(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 90;
        }
        String c = raw.trim().toUpperCase();
        if ("P".equals(c) || "PRIMARY".equals(c)) {
            return 90;
        }
        if ("S".equals(c) || "SECONDARY".equals(c)) {
            return 60;
        }
        if ("T".equals(c) || "TERTIARY".equals(c)) {
            return 35;
        }
        try {
            return clamp(Integer.parseInt(c));
        } catch (NumberFormatException e) {
            return 90;
        }
    }

    public static List<Rivalry> parseEncoded(String encoded) {
        ArrayList<Rivalry> list = new ArrayList<>();
        if (encoded == null || encoded.trim().isEmpty()) {
            return list;
        }
        String[] parts = encoded.split(";");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            String abbr;
            int strength;
            int colon = token.indexOf(':');
            if (colon < 0) {
                abbr = token;
                strength = 90;
            } else {
                abbr = token.substring(0, colon).trim();
                strength = parseStrengthToken(token.substring(colon + 1));
            }
            if (abbr.isEmpty()) {
                continue;
            }
            list.add(new Rivalry(abbr, strength));
        }
        return list;
    }

    public static String encode(List<Rivalry> rivalries) {
        if (rivalries == null || rivalries.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rivalries.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            Rivalry r = rivalries.get(i);
            sb.append(r.opponentAbbr).append(':').append(r.strength);
        }
        return sb.toString();
    }

    public static List<Rivalry> singlePrimary(String abbr) {
        if (abbr == null || abbr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Rivalry> list = new ArrayList<>(1);
        list.add(new Rivalry(abbr, 90));
        return list;
    }

    @Override
    public String toString() {
        return opponentAbbr + ":" + strength;
    }
}
