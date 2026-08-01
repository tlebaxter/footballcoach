package CFBsimPack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DepthChartCrossPosTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void rbCanFillFbSlotViaOvr() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.teamFBs.clear();
        Player rb = t.getRB(0);
        assertNotNull(rb);
        rb.ratings.rbk = 88;
        rb.ratings.stre = 85;
        rb.applyRatings(rb.ratings);

        OnFieldEleven eleven = OnFieldEleven.forOffense(t, "21");
        boolean hasFbRole = false;
        for (RoleTag role : eleven.roles) {
            if (role == RoleTag.FB) hasFbRole = true;
        }
        assertTrue(hasFbRole);
        assertTrue(eleven.players.size() >= 10);
        assertUniquePlayers(eleven);
    }

    @Test
    public void edgeSlotCanFillFromLb() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        // Nickel has an EDGE slot; 4-3 uses DE only
        t.setDefSystem(DefensiveSystem.NICKEL);
        t.teamEDGEs.clear();
        Player lb = t.getLB(0);
        assertNotNull(lb);
        lb.ratings.prs = 90;
        lb.ratings.spd = 85;
        lb.applyRatings(lb.ratings);

        OnFieldEleven def = OnFieldEleven.forDefense(t);
        boolean edgeFilled = false;
        for (int i = 0; i < def.roles.size(); i++) {
            if (def.roles.get(i) == RoleTag.EDGE) {
                edgeFilled = def.players.get(i) != null;
            }
        }
        assertTrue(edgeFilled);
        assertUniquePlayers(def);
    }

    @Test
    public void offenseElevenHasUniquePlayersAcrossPersonnel() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        for (String personnel : new String[] {"11", "12", "21", "20", "10"}) {
            assertUniquePlayers(OnFieldEleven.forOffense(t, personnel));
        }
    }

    @Test
    public void defenseElevenHasUniquePlayersAcrossSystems() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        for (DefensiveSystem sys : DefensiveSystem.values()) {
            t.setDefSystem(sys);
            assertUniquePlayers(OnFieldEleven.forDefense(t));
        }
    }

    @Test
    public void crossPosFillKeepsUniquePlayersWhenFbAndEdgeCleared() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.teamFBs.clear();
        t.teamEDGEs.clear();
        t.setDefSystem(DefensiveSystem.NICKEL);

        Player rb = t.getRB(0);
        assertNotNull(rb);
        rb.ratings.rbk = 88;
        rb.ratings.stre = 85;
        rb.applyRatings(rb.ratings);

        Player lb = t.getLB(0);
        assertNotNull(lb);
        lb.ratings.prs = 90;
        lb.ratings.spd = 85;
        lb.applyRatings(lb.ratings);

        assertUniquePlayers(OnFieldEleven.forOffense(t, "21"));
        assertUniquePlayers(OnFieldEleven.forDefense(t));
    }

    private static void assertUniquePlayers(OnFieldEleven eleven) {
        Set<Player> seen = new HashSet<>();
        for (Player p : eleven.players) {
            assertNotNull(p);
            assertTrue("Player appears in two on-field slots: " + p.name, seen.add(p));
        }
        assertEquals(eleven.players.size(), seen.size());
    }

    private static League createLeague() throws IOException {
        String csv = achijones.footballcoach.testing.FbsCsv.read();
        return new League(FIRST_NAMES, LAST_NAMES, csv);
    }
}
