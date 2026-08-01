package CFBsimPack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PositionBudgetBalancerTest {

    private static final String FIRST_NAMES = "Alex,Blake,Casey,Drew";
    private static final String LAST_NAMES = "Adams,Baker,Clark,Davis";

    @Test
    public void recordSpendReducesRemainingAndBlocksOverspend() throws Exception {
        League league = createLeague();
        Team team = league.teamList.get(0);
        int spendable = 1_000_000;
        PositionBudgetBalancer bal = new PositionBudgetBalancer(team, spendable);

        int qbBefore = bal.remaining("QB");
        assertTrue(qbBefore > 0);
        assertTrue(bal.canSpend("QB", Math.min(50_000, qbBefore), false));

        bal.recordSpend("QB", 50_000);
        assertEquals(qbBefore - 50_000, bal.remaining("QB"));

        int rem = bal.remaining("QB");
        assertFalse(bal.canSpend("QB", rem + 1_000_000, false));
    }

    @Test
    public void capsSumToSpendableBudget() throws Exception {
        League league = createLeague();
        Team team = league.teamList.get(0);
        int spendable = 800_000;
        PositionBudgetBalancer bal = new PositionBudgetBalancer(team, spendable);

        int total = 0;
        for (String pos : NilMoney.POSITIONS) {
            total += bal.remaining(pos);
        }
        assertEquals(spendable, total);
    }

    private League createLeague() throws IOException {
        String csv = achijones.footballcoach.testing.FbsCsv.read();
        return new League(FIRST_NAMES, LAST_NAMES, csv);
    }
}
