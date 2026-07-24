package CFBsimPack;

import java.util.Arrays;

/**
 * Persistent program identity plus cached scores used by the simulation.
 *
 * <p>The six factors move at different speeds. Derived values are recalculated
 * after a seed/load or offseason update so hot paths only read integers.</p>
 */
public final class ProgramProfile {
    private static final int MIN_FACTOR = 25;
    private static final int MAX_FACTOR = 99;
    private static final int HISTORY_YEARS = 5;
    private static final int DRAFT_HISTORY_YEARS = 3;

    public int tradition;
    public int fanbase;
    public int donors;
    public int footprint;
    public int pipeline;
    public int momentum;

    public int brandAttract;
    public int revSharePool;
    public int collectivePool;
    public int capitalPool;
    public int expectation;
    public int talentGravity;
    public int scheduleTier;
    public int programPower;

    public int diffProgramPower;
    public int diffMomentum;
    public int diffDonors;
    public int diffFanbase;
    public int diffTradition;
    public int diffFootprint;
    public int diffPipeline;

    private final int[] finishHistory;
    private final int[] draftScoreHistory;

    public ProgramProfile(
            int tradition,
            int fanbase,
            int donors,
            int footprint,
            int pipeline,
            int momentum,
            int mediaShare) {
        this.tradition = clampFactor(tradition);
        this.fanbase = clampFactor(fanbase);
        this.donors = clampFactor(donors);
        this.footprint = clampFactor(footprint);
        this.pipeline = clampFactor(pipeline);
        this.momentum = clampFactor(momentum);
        finishHistory = new int[HISTORY_YEARS];
        draftScoreHistory = new int[DRAFT_HISTORY_YEARS];
        Arrays.fill(draftScoreHistory, Math.max(0, (this.pipeline - 35) / 2));
        refreshDerived(mediaShare);
    }

    public void refreshDerived(int mediaShare) {
        int media = clampScore(mediaShare);
        brandAttract = weighted(
                tradition, 0.35,
                fanbase, 0.25,
                momentum, 0.25,
                pipeline, 0.15);
        revSharePool = weighted(
                media, 0.70,
                fanbase, 0.20,
                tradition, 0.10);
        collectivePool = weighted(
                donors, 0.55,
                fanbase, 0.25,
                momentum, 0.20);
        capitalPool = weighted(revSharePool, 0.48, collectivePool, 0.52);
        expectation = weighted(
                tradition, 0.30,
                donors, 0.25,
                momentum, 0.25,
                media, 0.20);
        talentGravity = weighted(
                brandAttract, 0.30,
                footprint, 0.30,
                capitalPool, 0.25,
                pipeline, 0.15);
        scheduleTier = weighted(brandAttract, 0.60, capitalPool, 0.40);
        programPower = weighted(
                brandAttract, 0.40,
                capitalPool, 0.35,
                talentGravity, 0.25);
    }

    public int expectedPollFinish(int teamCount) {
        int count = Math.max(1, teamCount);
        return 1 + (int) Math.round((100 - expectation) / 100.0 * (count - 1));
    }

    public void beginAnnualUpdate() {
        diffProgramPower = 0;
        diffMomentum = 0;
        diffDonors = 0;
        diffFanbase = 0;
        diffTradition = 0;
        diffFootprint = 0;
        diffPipeline = 0;
    }

    public void updateForSeason(
            int actualPollFinish,
            int teamCount,
            boolean nationalChampion,
            int rivalryMomentum,
            int draftClassScore,
            int mediaShare) {
        beginAnnualUpdate();
        int oldPower = programPower;
        int oldMomentum = momentum;
        int oldDonors = donors;
        int oldFanbase = fanbase;
        int oldTradition = tradition;
        int oldFootprint = footprint;
        int oldPipeline = pipeline;

        int expectedFinish = expectedPollFinish(teamCount);
        int overperformance = expectedFinish - Math.max(1, actualPollFinish);
        int performanceMove = clamp((int) Math.round(overperformance / 4.5), -8, 8);
        if (nationalChampion) {
            performanceMove = Math.max(performanceMove, 8);
        }
        momentum = clampFactor(momentum + clamp(performanceMove + rivalryMomentum, -10, 10));

        int donorMove = clamp((int) Math.round(performanceMove * 0.55), -5, 5);
        if (nationalChampion) {
            donorMove = Math.max(donorMove, 6);
        }
        donors = clampFactor(donors + donorMove);

        push(finishHistory, actualPollFinish);
        int eliteFinishes = countAtMost(finishHistory, 15);
        int bottomHalfFinishes = countAtLeast(finishHistory, Math.max(2, teamCount / 2));
        if (nationalChampion || eliteFinishes >= 3) {
            tradition = clampFactor(tradition + 1);
        } else if (bottomHalfFinishes >= 5 && performanceMove < 0) {
            tradition = clampFactor(tradition - 1);
        }

        if (momentum >= 82 && performanceMove > 0) {
            fanbase = clampFactor(fanbase + Math.min(3, 1 + performanceMove / 4));
        } else if (momentum <= 45 && performanceMove < 0) {
            fanbase = clampFactor(fanbase - Math.min(3, 1 + Math.abs(performanceMove) / 4));
        }

        if (eliteFinishes >= 3 && fanbase >= 75) {
            footprint = clampFactor(footprint + 1);
        } else if (bottomHalfFinishes >= 5 && fanbase <= 45) {
            footprint = clampFactor(footprint - 1);
        }

        push(draftScoreHistory, Math.max(0, draftClassScore));
        int averageDraftScore = roundedAverage(draftScoreHistory);
        int pipelineTarget = clampFactor(35 + Math.min(64, averageDraftScore * 2));
        int pipelineMove = clamp((int) Math.round((pipelineTarget - pipeline) / 6.0), -5, 5);
        pipeline = clampFactor(pipeline + pipelineMove);

        refreshDerived(mediaShare);
        diffProgramPower = programPower - oldPower;
        diffMomentum = momentum - oldMomentum;
        diffDonors = donors - oldDonors;
        diffFanbase = fanbase - oldFanbase;
        diffTradition = tradition - oldTradition;
        diffFootprint = footprint - oldFootprint;
        diffPipeline = pipeline - oldPipeline;
    }

    public int developmentBonus() {
        if (pipeline >= 90) return 2;
        if (pipeline >= 75) return 1;
        if (pipeline <= 35) return -1;
        return 0;
    }

    public String finishHistoryCsv() {
        return join(finishHistory);
    }

    public String draftHistoryCsv() {
        return join(draftScoreHistory);
    }

    public String annualDeltaCsv() {
        return diffProgramPower + ":" + diffMomentum + ":" + diffDonors + ":"
                + diffFanbase + ":" + diffTradition + ":" + diffFootprint + ":"
                + diffPipeline;
    }

    public void restoreHistory(String finishes, String draftScores) {
        restore(finishHistory, finishes);
        restore(draftScoreHistory, draftScores);
    }

    public void restoreAnnualDeltas(String encoded) {
        String[] values = encoded != null ? encoded.split(":") : new String[0];
        if (values.length != 7) return;
        diffProgramPower = Integer.parseInt(values[0]);
        diffMomentum = Integer.parseInt(values[1]);
        diffDonors = Integer.parseInt(values[2]);
        diffFanbase = Integer.parseInt(values[3]);
        diffTradition = Integer.parseInt(values[4]);
        diffFootprint = Integer.parseInt(values[5]);
        diffPipeline = Integer.parseInt(values[6]);
    }

    private static int weighted(int first, double firstWeight, int second, double secondWeight) {
        return clampScore((int) Math.round(first * firstWeight + second * secondWeight));
    }

    private static int weighted(
            int first, double firstWeight,
            int second, double secondWeight,
            int third, double thirdWeight) {
        return clampScore((int) Math.round(
                first * firstWeight + second * secondWeight + third * thirdWeight));
    }

    private static int weighted(
            int first, double firstWeight,
            int second, double secondWeight,
            int third, double thirdWeight,
            int fourth, double fourthWeight) {
        return clampScore((int) Math.round(
                first * firstWeight
                        + second * secondWeight
                        + third * thirdWeight
                        + fourth * fourthWeight));
    }

    private static int clampFactor(int value) {
        return clamp(value, MIN_FACTOR, MAX_FACTOR);
    }

    private static int clampScore(int value) {
        return clamp(value, MIN_FACTOR, MAX_FACTOR);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void push(int[] history, int value) {
        System.arraycopy(history, 1, history, 0, history.length - 1);
        history[history.length - 1] = value;
    }

    private static int countAtMost(int[] values, int maximum) {
        int count = 0;
        for (int value : values) {
            if (value > 0 && value <= maximum) count++;
        }
        return count;
    }

    private static int countAtLeast(int[] values, int minimum) {
        int count = 0;
        for (int value : values) {
            if (value >= minimum) count++;
        }
        return count;
    }

    private static int roundedAverage(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return (int) Math.round(total / (double) values.length);
    }

    private static String join(int[] values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(':');
            out.append(values[i]);
        }
        return out.toString();
    }

    private static void restore(int[] target, String encoded) {
        Arrays.fill(target, 0);
        if (encoded == null || encoded.isEmpty()) return;
        String[] values = encoded.split(":");
        for (int i = 0; i < target.length && i < values.length; i++) {
            target[i] = Integer.parseInt(values[i]);
        }
    }
}
