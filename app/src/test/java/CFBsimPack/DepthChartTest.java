package CFBsimPack;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DepthChartTest {

    private static final String FIRST_NAMES = "A,B,C,D,E,F,G,H,I,J";
    private static final String LAST_NAMES = "K,L,M,N,O,P,Q,R,S,T";

    @Test
    public void setDepthChartPreservesExactOrder() throws Exception {
        League league = createLeague();
        Team team = league.userTeam;
        ArrayList<Player> qbs = new ArrayList<>(team.teamQBs);
        assertTrue(qbs.size() >= 3);

        ArrayList<Player> reversed = new ArrayList<>();
        for (int i = qbs.size() - 1; i >= 0; i--) {
            reversed.add(qbs.get(i));
        }
        team.setDepthChart(reversed, 0);

        for (int i = 0; i < reversed.size(); i++) {
            assertEquals(reversed.get(i), team.teamQBs.get(i));
        }
    }

    @Test
    public void lockedPlayerKeepsSlotThroughSort() throws Exception {
        League league = createLeague();
        Team team = league.userTeam;
        assertTrue(team.teamQBs.size() >= 3);

        Player weakest = team.teamQBs.get(0);
        for (Player qb : team.teamQBs) {
            if (qb.ratOvr < weakest.ratOvr) weakest = qb;
        }
        // Put weakest at QB1 and lock him; unlocked should fill around him by OVR
        ArrayList<Player> order = new ArrayList<>();
        order.add(weakest);
        for (Player qb : team.teamQBs) {
            if (qb != weakest) order.add(qb);
        }
        team.setDepthChart(order, 0);
        weakest.depthLocked = true;

        team.sortPositionDepth(0);

        assertEquals(weakest, team.teamQBs.get(0));
        for (int i = 1; i < team.teamQBs.size() - 1; i++) {
            assertTrue(team.teamQBs.get(i).ratOvr >= team.teamQBs.get(i + 1).ratOvr
                    || team.teamQBs.get(i + 1).isInjured);
        }
    }

    @Test
    public void injuredLockedPlayerDoesNotStayPinned() throws Exception {
        League league = createLeague();
        Team team = league.userTeam;
        assertTrue(team.teamQBs.size() >= 2);

        Player top = team.teamQBs.get(0);
        top.depthLocked = true;
        top.isInjured = true;

        team.sortPositionDepth(0);

        assertFalse(team.teamQBs.get(0).isInjured);
        assertTrue(team.teamQBs.contains(top));
    }

    @Test
    public void setDepthLocksTargetsStartersOrBench() throws Exception {
        League league = createLeague();
        Team team = league.userTeam;
        assertTrue(team.teamRBs.size() >= 3);

        team.setDepthLocks(1, true, true);
        assertTrue(team.teamRBs.get(0).depthLocked);
        assertTrue(team.teamRBs.get(1).depthLocked);
        assertFalse(team.teamRBs.get(2).depthLocked);

        team.setDepthLocks(1, false, true);
        assertTrue(team.teamRBs.get(2).depthLocked);

        team.setDepthLocks(1, true, false);
        assertFalse(team.teamRBs.get(0).depthLocked);
    }

    @Test
    public void depthLockedPersistsInRosterStatusSave() {
        Player p = new Player();
        p.rosterStatus = RosterStatus.SCHOLARSHIP;
        p.depthLocked = true;
        String saved = p.rosterStatusSave();
        Player loaded = new Player();
        loaded.loadRosterStatus(saved);
        assertTrue(loaded.depthLocked);

        p.depthLocked = false;
        loaded.loadRosterStatus(p.rosterStatusSave());
        assertFalse(loaded.depthLocked);
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        League league = new League(FIRST_NAMES, LAST_NAMES, csv);
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;
        return league;
    }
}
