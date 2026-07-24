package com.example.achijones.footballcoach;

import CFBsimPack.League;
import CFBsimPack.LeagueOffseason;
import CFBsimPack.NilMoney;
import CFBsimPack.OffseasonSession;
import CFBsimPack.Player;
import CFBsimPack.ProgramOffers;
import CFBsimPack.RosterStatus;
import CFBsimPack.Team;
import CFBsimPack.TransferReason;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OffseasonContractsTest {

    private static final String FIRST_NAMES =
            "Alex,Blake,Casey,Drew,Evan,Frankie,Gray,Hayden";
    private static final String LAST_NAMES =
            "Adams,Baker,Clark,Davis,Evans,Foster,Green,Hill";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void maxContractYearsCapsByEligibility() {
        Player fr = new Player();
        fr.year = 1;
        assertEquals(4, ProgramOffers.maxContractYears(fr));
        Player so = new Player();
        so.year = 2;
        assertEquals(4, ProgramOffers.maxContractYears(so));
        Player jr = new Player();
        jr.year = 3;
        assertEquals(3, ProgramOffers.maxContractYears(jr));
        Player sr = new Player();
        sr.year = 4;
        assertEquals(2, ProgramOffers.maxContractYears(sr));
    }

    @Test
    public void youthPremiumMakesYoungerMoreExpensiveThanSenior() {
        Player young = new Player();
        young.year = 1;
        young.ratOvr = 78;
        young.ratPot = 92;
        young.position = "WR";

        Player senior = new Player();
        senior.year = 4;
        senior.ratOvr = 78;
        senior.ratPot = 80;
        senior.position = "WR";

        assertTrue(NilMoney.youthPremium(young) > NilMoney.youthPremium(senior));
        assertTrue(NilMoney.marketValue(young) > NilMoney.marketValue(senior));
    }

    @Test
    public void draftProjectionLocksTopThreeRounds() {
        Player elite = new Player();
        elite.year = 3;
        elite.ratOvr = 95;
        elite.position = "QB";
        elite.wonHeisman = true;
        int round = ProgramOffers.projectDraftRound(elite);
        assertTrue(round >= 1 && round <= 3);
        assertTrue(ProgramOffers.isLockedDraftRound(round));
        assertFalse(ProgramOffers.canPayToStay(elite));
    }

    @Test
    public void midRoundDraftCanPayToStay() {
        Player mid = new Player();
        mid.year = 3;
        mid.ratOvr = 86;
        mid.position = "WR";
        mid.projectedDraftRound = 4;
        assertTrue(ProgramOffers.canPayToStay(mid));
        assertTrue(ProgramOffers.draftStayBonus(mid, null) >= 100000);
    }

    @Test
    public void buyoutScalesWithRemainingYears() {
        Player p = new Player();
        p.year = 2;
        p.ratOvr = 80;
        p.ratPot = 88;
        p.rosterStatus = RosterStatus.SCHOLARSHIP_PLUS_NIL;
        p.nilDealAmount = 500000;
        p.contractYearsRemaining = 3;
        p.contractLength = 4;
        int longBuy = NilMoney.buyoutCost(p, 80);
        p.contractYearsRemaining = 1;
        int shortBuy = NilMoney.buyoutCost(p, 80);
        assertTrue(longBuy > shortBuy);
    }

    @Test
    public void contractSaveLoadRoundTrip() {
        Player p = new Player();
        p.rosterStatus = RosterStatus.SCHOLARSHIP_PLUS_NIL;
        p.nilDealAmount = 250000;
        p.contractYearsRemaining = 2;
        p.contractLength = 3;
        p.retainedThisOffseason = true;
        String saved = p.rosterStatusSave();
        Player q = new Player();
        q.loadRosterStatus(saved);
        assertEquals(RosterStatus.SCHOLARSHIP_PLUS_NIL, q.rosterStatus);
        assertEquals(250000, q.nilDealAmount);
        assertEquals(2, q.contractYearsRemaining);
        assertEquals(3, q.contractLength);
        assertTrue(q.retainedThisOffseason);
    }

    @Test
    public void midOffseasonSaveRoundTripRestoresPhaseBudgetAndPortal() throws Exception {
        League league = createLeague();
        Team user = league.userTeam != null ? league.userTeam : league.teamList.get(0);
        league.userTeam = user;
        user.userControlled = true;
        LeagueOffseason off = new LeagueOffseason(league);
        off.grantAllBudgets();
        int budgetBefore = user.recruitMoney;
        Player keep = user.getAllPlayers().get(0);
        keep.retainedThisOffseason = true;
        keep.applyOffer(RosterStatus.SCHOLARSHIP, 0, 2);
        off.buildTransferPortal();
        assertTrue(off.transferPortal.size() > 0);
        Player portalSample = off.transferPortal.get(0);
        String portalName = portalSample.name;
        OffseasonSession.begin(league, off, OffseasonSession.Phase.PORTAL);

        java.io.File saveFile = temporaryFolder.newFile("offseason_roundtrip.cfb");
        assertTrue(league.saveLeague(saveFile));
        OffseasonSession.clear();

        League loaded = new League(saveFile, FIRST_NAMES, LAST_NAMES);
        assertTrue(loaded.loadedInOffseason);
        assertEquals(OffseasonSession.Phase.PORTAL, loaded.loadedOffseasonPhase);
        assertTrue(OffseasonSession.ready());
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase);
        assertEquals(budgetBefore, loaded.userTeam.recruitMoney);
        boolean foundPortal = false;
        for (Player p : OffseasonSession.offseason.transferPortal) {
            if (portalName.equals(p.name)) {
                foundPortal = true;
                break;
            }
        }
        assertTrue(foundPortal);
        boolean foundRetained = false;
        for (Player p : loaded.userTeam.getAllPlayers()) {
            if (keep.name.equals(p.name) && p.retainedThisOffseason) {
                foundRetained = true;
                break;
            }
        }
        assertTrue(foundRetained);
        OffseasonSession.clear();
    }

    @Test
    public void retainedPlayerSurvivesPortalBuild() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        t.userControlled = false;
        Player keep = null;
        for (Player p : t.getAllPlayers()) {
            if (p.year < 5) {
                keep = p;
                break;
            }
        }
        assertTrue(keep != null);
        keep.portalRiskTier = 3;
        keep.transferReason = TransferReason.PLAYING_TIME;
        keep.retainedThisOffseason = true;
        keep.applyOffer(RosterStatus.SCHOLARSHIP, 0, 2);

        LeagueOffseason off = new LeagueOffseason(league);
        off.buildTransferPortal();
        assertFalse(off.transferPortal.contains(keep));
        assertTrue(t.getAllPlayers().contains(keep));
        assertEquals(0, keep.portalRiskTier);
    }

    @Test
    public void canAffordContractChecksFutureYears() throws Exception {
        League league = createLeague();
        Team t = league.userTeam != null ? league.userTeam : league.teamList.get(0);
        t.recruitMoney = 200000;
        assertTrue(t.canAffordContract(RosterStatus.SCHOLARSHIP, 0, 1));
        Player qb = t.teamQBs.isEmpty() ? null : t.teamQBs.get(0);
        if (qb != null) {
            qb.rosterStatus = RosterStatus.SCHOLARSHIP_PLUS_NIL;
            qb.nilDealAmount = t.projectedBudget(1);
            qb.contractYearsRemaining = 2;
            // Future NIL already consumes the projected purse — multi-year NIL should fail
            assertFalse(t.canAffordContract(RosterStatus.SCHOLARSHIP_PLUS_NIL, 150000, 3));
        }
    }

    @Test
    public void grantYearlyBudgetLeavesPlayablePurse() throws Exception {
        League league = createLeague();
        Team t = league.teamList.get(0);
        for (Player p : t.getAllPlayers()) {
            if (p.ratOvr >= 75) {
                p.applyOffer(RosterStatus.SCHOLARSHIP_PLUS_NIL, 400000, 3);
            }
        }
        t.grantYearlyBudget();
        assertTrue("Expected playable budget after grant, got " + t.recruitMoney, t.recruitMoney > 100000);
    }

    @Test
    public void noneReasonIsNotAtRisk() {
        assertFalse(TransferReason.NONE.isIssue());
        assertTrue(TransferReason.PLAYING_TIME.isIssue());
    }

    private League createLeague() throws IOException {
        Path csvPath = Paths.get("app/src/main/assets/fbs_2026.csv");
        if (!Files.exists(csvPath)) {
            csvPath = Paths.get("src/main/assets/fbs_2026.csv");
        }
        String csv = new String(Files.readAllBytes(csvPath), StandardCharsets.UTF_8);
        return new League(FIRST_NAMES, LAST_NAMES, csv);
    }
}
