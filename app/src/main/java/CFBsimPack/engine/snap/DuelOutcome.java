package CFBsimPack.engine.snap;

/** Result of a stochastic one-on-one contest. */
public final class DuelOutcome {
    public enum Result { WIN, HOLD, LOSS }

    public final Result result;
    /** Positive favors offense/blocker/receiver; negative favors defense. */
    public final double margin;

    public DuelOutcome(Result result, double margin) {
        this.result = result != null ? result : Result.HOLD;
        this.margin = margin;
    }

    public boolean isWin() {
        return result == Result.WIN;
    }

    public boolean isLoss() {
        return result == Result.LOSS;
    }
}
