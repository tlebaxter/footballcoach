package CFBsimPack.engine;

/**
 * Transactional snap result: provisional state + optional foul before commit.
 */
public final class PendingPlay {
    public final PlayResult result;
    public final PlayState before;
    public final PlayState after;
    public PenaltyCatalog.Foul foul;
    public boolean foulAccepted;

    public PendingPlay(PlayResult result, PlayState before, PlayState after) {
        this.result = result;
        this.before = before;
        this.after = after;
    }
}
