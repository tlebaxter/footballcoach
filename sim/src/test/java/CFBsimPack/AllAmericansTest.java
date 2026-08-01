package CFBsimPack;

import achijones.footballcoach.testing.FbsCsv;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression: all-conference lists omit FB/TE when those pools are empty.
 * getAllAmericans must partition by position, not fixed indexes.
 */
public class AllAmericansTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void getAllConfPlayersOmitsMissingFbAndTe() {
        League league = leagueWithNoFbOrTe();
        Conference conf = league.conferences.get(0);
        conf.allConfPlayers.clear();

        ArrayList<Player> allConf = conf.getAllConfPlayers();
        assertNotNull(allConf);
        assertFalse(allConf.isEmpty());
        for (Player p : allConf) {
            assertFalse("FB", "FB".equals(p.position));
            assertFalse("TE", "TE".equals(p.position));
        }
        assertTrue(hasPosition(allConf, "QB"));
        assertTrue(hasPosition(allConf, "RB"));
        assertTrue(hasPosition(allConf, "WR"));
    }

    @Test
    public void getAllAmericansSurvivesMissingFbAndTe() {
        League league = leagueWithNoFbOrTe();
        for (Conference c : league.conferences) {
            c.allConfPlayers.clear();
        }
        league.allAmericans.clear();

        ArrayList<Player> americans = league.getAllAmericans();
        assertNotNull(americans);
        assertFalse(americans.isEmpty());
        assertTrue(hasPosition(americans, "QB"));
        assertTrue(hasPosition(americans, "RB"));
        assertTrue(hasPosition(americans, "WR"));
        for (Player p : americans) {
            assertFalse("FB should be omitted when none exist", "FB".equals(p.position));
            assertFalse("TE should be omitted when none exist", "TE".equals(p.position));
            assertTrue(p.wonAllAmerican);
        }
    }

    private static League leagueWithNoFbOrTe() {
        League league = new League(FIRST_NAMES, LAST_NAMES, FbsCsv.read());
        for (Team t : league.teamList) {
            t.teamFBs.clear();
            t.teamTEs.clear();
        }
        return league;
    }

    private static boolean hasPosition(ArrayList<Player> players, String position) {
        for (Player p : players) {
            if (position.equals(p.position)) return true;
        }
        return false;
    }
}
