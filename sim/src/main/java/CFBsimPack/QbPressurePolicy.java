package CFBsimPack;

/**
 * Per-situation QB pocket-pressure preferences for a team.
 * Slots: normal, convert, protectLead, lateTrailing, backedUp.
 */
public final class QbPressurePolicy {

    public enum Slot {
        NORMAL("Normal"),
        CONVERT("Convert"),
        PROTECT_LEAD("Protect Lead"),
        LATE_TRAILING("Late Trailing"),
        BACKED_UP("Backed Up");

        public final String displayName;

        Slot(String displayName) {
            this.displayName = displayName;
        }
    }

    public final PressureResponse normal;
    public final PressureResponse convert;
    public final PressureResponse protectLead;
    public final PressureResponse lateTrailing;
    public final PressureResponse backedUp;

    public QbPressurePolicy(
            PressureResponse normal,
            PressureResponse convert,
            PressureResponse protectLead,
            PressureResponse lateTrailing,
            PressureResponse backedUp
    ) {
        this.normal = normal != null ? normal : PressureResponse.AUTO;
        this.convert = convert != null ? convert : PressureResponse.TAKE_THE_FIRST_DOWN;
        this.protectLead = protectLead != null ? protectLead : PressureResponse.SLIDE_SECURE;
        this.lateTrailing = lateTrailing != null ? lateTrailing : PressureResponse.SCRAMBLE_FOR_IT;
        this.backedUp = backedUp != null ? backedUp : PressureResponse.THROW_IT_AWAY;
    }

    public static QbPressurePolicy defaults() {
        return new QbPressurePolicy(
                PressureResponse.AUTO,
                PressureResponse.TAKE_THE_FIRST_DOWN,
                PressureResponse.SLIDE_SECURE,
                PressureResponse.SCRAMBLE_FOR_IT,
                PressureResponse.THROW_IT_AWAY
        );
    }

    public PressureResponse forSlot(Slot slot) {
        if (slot == null) return normal;
        switch (slot) {
            case CONVERT:
                return convert;
            case PROTECT_LEAD:
                return protectLead;
            case LATE_TRAILING:
                return lateTrailing;
            case BACKED_UP:
                return backedUp;
            case NORMAL:
            default:
                return normal;
        }
    }

    public QbPressurePolicy copyWith(Slot slot, PressureResponse response) {
        PressureResponse r = response != null ? response : PressureResponse.AUTO;
        switch (slot != null ? slot : Slot.NORMAL) {
            case CONVERT:
                return new QbPressurePolicy(normal, r, protectLead, lateTrailing, backedUp);
            case PROTECT_LEAD:
                return new QbPressurePolicy(normal, convert, r, lateTrailing, backedUp);
            case LATE_TRAILING:
                return new QbPressurePolicy(normal, convert, protectLead, r, backedUp);
            case BACKED_UP:
                return new QbPressurePolicy(normal, convert, protectLead, lateTrailing, r);
            case NORMAL:
            default:
                return new QbPressurePolicy(r, convert, protectLead, lateTrailing, backedUp);
        }
    }

    /** Colon-separated ordinals for league save. */
    public String encode() {
        return normal.ordinal() + ":"
                + convert.ordinal() + ":"
                + protectLead.ordinal() + ":"
                + lateTrailing.ordinal() + ":"
                + backedUp.ordinal();
    }

    public static QbPressurePolicy parse(String encoded) {
        if (encoded == null || encoded.isEmpty() || "null".equalsIgnoreCase(encoded)) {
            return defaults();
        }
        String[] parts = encoded.split(":");
        if (parts.length < 5) return defaults();
        try {
            return new QbPressurePolicy(
                    PressureResponse.fromOrdinalSafe(Integer.parseInt(parts[0].trim())),
                    PressureResponse.fromOrdinalSafe(Integer.parseInt(parts[1].trim())),
                    PressureResponse.fromOrdinalSafe(Integer.parseInt(parts[2].trim())),
                    PressureResponse.fromOrdinalSafe(Integer.parseInt(parts[3].trim())),
                    PressureResponse.fromOrdinalSafe(Integer.parseInt(parts[4].trim()))
            );
        } catch (NumberFormatException e) {
            return defaults();
        }
    }
}
