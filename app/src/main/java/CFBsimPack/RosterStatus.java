package CFBsimPack;

/**
 * How a player is on the roster. NIL is only allowed with a scholarship.
 */
public enum RosterStatus {
    PWO,
    SCHOLARSHIP,
    SCHOLARSHIP_PLUS_NIL;

    public boolean usesScholarship() {
        return this == SCHOLARSHIP || this == SCHOLARSHIP_PLUS_NIL;
    }

    public String displayName() {
        switch (this) {
            case PWO:
                return "PWO";
            case SCHOLARSHIP:
                return "Scholarship";
            case SCHOLARSHIP_PLUS_NIL:
                return "Scholarship+NIL";
            default:
                return name();
        }
    }

    public static RosterStatus fromString(String s) {
        if (s == null) return SCHOLARSHIP;
        switch (s.trim().toUpperCase()) {
            case "PWO":
                return PWO;
            case "SCHOLARSHIP_PLUS_NIL":
            case "SCHOLARSHIP+NIL":
            case "NIL":
                return SCHOLARSHIP_PLUS_NIL;
            case "SCHOLARSHIP":
            default:
                return SCHOLARSHIP;
        }
    }
}
