package CFBsimPack.engine;

import CFBsimPack.Player;

/**
 * Transactional snap result: provisional state + optional foul before commit.
 */
public final class PendingPlay {
    public final PlayResult result;
    public final PlayState before;
    public final PlayState after;
    public PenaltyCatalog.Foul foul;
    public boolean foulAccepted;
    /** Offense-perspective yard line for spot fouls; 0 means unset (use previous spot). */
    public int foulSpotYardLine;
    public Player ejectedPlayer;
    /** Rare mid-play injury that can arm a 10-second runoff. */
    public boolean injuryStoppage;

    public PendingPlay(PlayResult result, PlayState before, PlayState after) {
        this.result = result;
        this.before = before;
        this.after = after;
    }
}
