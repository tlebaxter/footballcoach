package CFBsimPack;

/**
 * Why a player entered the transfer portal.
 */
public enum TransferReason {
    NONE("No issues"),
    PLAYING_TIME("Seeking playing time"),
    MOVE_UP("Moving up in competition"),
    COACHING_CHANGE("Coaching change"),
    SCHEME_FIT("Seeking better scheme fit"),
    WINNING("Seeking a winning program"),
    PROGRAM_FREEFALL("Leaving a declining program"),
    TITLE_CHASE("Chasing a championship"),
    INJURY_COMEBACK("Fresh start after injury"),
    BETTER_FIT("Seeking a better fit");

    public final String label;

    TransferReason(String label) {
        this.label = label;
    }

    public boolean isIssue() {
        return this != NONE;
    }

    public static TransferReason fromString(String s) {
        if (s == null || s.isEmpty()) return NONE;
        try {
            return TransferReason.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return BETTER_FIT;
        }
    }
}
