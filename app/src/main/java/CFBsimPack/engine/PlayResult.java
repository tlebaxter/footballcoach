package CFBsimPack.engine;

public final class PlayResult {
    public int yardsGained;
    public int clockBurned;
    public boolean turnover;
    public boolean touchdown;
    public boolean scoreFg;
    public boolean scoreXp;
    public boolean safety;
    public boolean possessionChanged;
    public boolean firstDown;
    public boolean incomplete;
    public boolean stoppedClock;
    public boolean fairCatch;
    public boolean touchback;
    public boolean puntBlocked;
    public int returnYards;
    public boolean returnTd;
    public String returnerName = "";
    public String logLine = "";
    public OffensePlay playType;

    public static PlayResult logOnly(String line, int clock) {
        PlayResult r = new PlayResult();
        r.logLine = line;
        r.clockBurned = clock;
        return r;
    }
}
