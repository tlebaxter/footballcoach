package CFBsimPack;

public enum Formation {
    I_FORM("I-Form"),
    SINGLEBACK("Singleback"),
    SHOTGUN("Shotgun"),
    PISTOL("Pistol"),
    EMPTY("Empty"),
    TRIPS("Trips"),
    SLOT("Slot"),
    JUMBO("Jumbo"),
    WISHBONE("Wishbone"),
    ACE("Ace");

    public final String displayName;

    Formation(String displayName) {
        this.displayName = displayName;
    }
}
