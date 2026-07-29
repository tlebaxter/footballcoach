package CFBsimPack.engine.snap;

import java.util.Random;

/**
 * Stochastic one-on-one contests. Rating edge shifts odds; never auto-wins.
 * At ±25 rating, favorite wins roughly ~75% (with HOLD as middle band).
 */
public final class DuelResolver {

    /** Logistic steepness: larger → softer edges. */
    public static final double K = 22.0;
    /** Gaussian noise on the latent score. */
    public static final double SIGMA = 1.05;
    /** |latent| above this → WIN/LOSS; below → HOLD. */
    public static final double HOLD_BAND = 0.28;

    private final Random rng;

    public DuelResolver(Random rng) {
        this.rng = rng != null ? rng : new Random();
    }

    /**
     * Contest from offense/blocker/receiver perspective vs defense.
     *
     * @param offRating 0–100 offensive side rating
     * @param defRating 0–100 defensive side rating
     */
    public DuelOutcome contest(int offRating, int defRating) {
        double diff = (offRating - defRating) / K;
        double noise = rng.nextGaussian() * SIGMA;
        double latent = diff + noise;
        DuelOutcome.Result result;
        if (latent > HOLD_BAND) {
            result = DuelOutcome.Result.WIN;
        } else if (latent < -HOLD_BAND) {
            result = DuelOutcome.Result.LOSS;
        } else {
            result = DuelOutcome.Result.HOLD;
        }
        return new DuelOutcome(result, latent);
    }

    /** Win probability for offense before noise (for tests / tuning). */
    public static double winProbability(int offRating, int defRating) {
        double z = (offRating - defRating) / K;
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
