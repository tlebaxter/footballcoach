package achijones.footballcoach.ui.talenthub

import CFBsimPack.NilMoney
import CFBsimPack.Player
import CFBsimPack.RosterStatus
import CFBsimPack.Team
import achijones.footballcoach.testing.LeagueFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DepthBoardModelsTest {

    private lateinit var team: Team

    @Before
    fun setUp() {
        val league = LeagueFixtures.createLeagueWithUser()
        team = league.userTeam
        team.recruitMoney = 5_000_000
    }

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
    }

    @Test
    fun buildDepthBoard_countsPositionsSchollyPwoAndNil() {
        val wr = team.teamWRs.first()
        wr.applyOffer(RosterStatus.SCHOLARSHIP_PLUS_NIL, 250_000, 2)
        val pwo = team.teamWRs.getOrNull(1)
        pwo?.applyOffer(RosterStatus.PWO, 0, 1)

        val board = buildDepthBoard(team)
        val wrRow = board.positions.first { it.position == "WR" }

        assertEquals(team.teamWRs.size, wrRow.have)
        assertEquals(NilMoney.sugFor("WR"), wrRow.sug)
        assertTrue(wrRow.scholly >= 1)
        if (pwo != null) {
            assertTrue(wrRow.pwo >= 1)
        }
        assertTrue(wrRow.nilSpend >= 250_000)
        assertTrue(wrRow.contextLine().contains("NIL "))
        assertEquals(team.scholarshipCount, board.schollyUsed)
        assertEquals(team.rosterCount, board.rosterUsed)
        assertTrue(board.pwoLabel.startsWith("PWO "))
        assertEquals(NilMoney.POSITIONS.size, board.positions.size)
    }

    @Test
    fun portalAdd_scholarshipIncreasesRosterSchollyAndDropsCash() {
        // Keep WR NIL small so format() can show a visible delta, and free a scholly slot.
        for (p in team.teamWRs) {
            if (p.rosterStatus == RosterStatus.SCHOLARSHIP_PLUS_NIL) {
                p.applyOffer(RosterStatus.SCHOLARSHIP, 0, 1)
            }
        }
        val schollyDonor = team.allPlayers.first { it.rosterStatus != null && it.rosterStatus.usesScholarship() }
        schollyDonor.applyOffer(RosterStatus.PWO, 0, 1)
        assertTrue(team.canAwardScholarship())
        val before = buildDepthBoard(team)
        val recruit = newRecruit("WR", 85)
        val nil = 200_000
        val cost = team.nilPurseCost(RosterStatus.SCHOLARSHIP_PLUS_NIL, nil)
        assertTrue(cost > 0)
        val impact = buildOfferImpact(
            team = team,
            player = recruit,
            kind = OfferImpactKind.PORTAL_OR_HS,
            proposedStatus = RosterStatus.SCHOLARSHIP_PLUS_NIL,
            proposedNil = nil,
            years = 3,
        )

        val rosterLine = impact.lines.first { it.label == "Roster" }
        val schollyLine = impact.lines.first { it.label == "Scholarships" }
        val cashLine = impact.lines.first { it.label == "Purse" }
        val depthLine = impact.lines.first { it.label == "WR depth" }
        val nilLine = impact.lines.first { it.label == "WR NIL" }
        val wrBefore = before.positions.first { it.position == "WR" }

        assertEquals("${before.rosterUsed}/${before.rosterCap}", rosterLine.before)
        assertEquals("${before.rosterUsed + 1}/${before.rosterCap}", rosterLine.after)
        assertEquals("${before.schollyUsed}/${before.schollyCap}", schollyLine.before)
        assertEquals("${before.schollyUsed + 1}/${before.schollyCap}", schollyLine.after)
        assertTrue(cashLine.changed)
        assertEquals(
            NilMoney.format(before.cash - cost),
            cashLine.after,
        )
        assertEquals(NilMoney.format(wrBefore.nilSpend), nilLine.before)
        assertEquals(NilMoney.format(wrBefore.nilSpend + nil), nilLine.after)
        assertTrue(nilLine.changed)
        assertTrue(depthLine.after.startsWith("${wrBefore.have + 1}/"))
        assertNull(impact.blockedReason)
    }

    @Test
    fun portalAdd_pwoIncreasesPwoNotScholly() {
        val before = buildDepthBoard(team)
        val recruit = newRecruit("RB", 55)
        val impact = buildOfferImpact(
            team = team,
            player = recruit,
            kind = OfferImpactKind.PORTAL_OR_HS,
            proposedStatus = RosterStatus.PWO,
            proposedNil = 0,
            years = 1,
        )

        val schollyLine = impact.lines.first { it.label == "Scholarships" }
        val pwoLine = impact.lines.first { it.label == "PWOs" }
        assertFalse(schollyLine.changed)
        assertEquals(before.pwoCount.toString(), pwoLine.before)
        assertEquals((before.pwoCount + 1).toString(), pwoLine.after)
        assertNull(impact.blockedReason)
    }

    @Test
    fun retentionRenew_rosterUnchangedStatusSwapReflected() {
        val player = team.teamQBs.first()
        player.applyOffer(RosterStatus.PWO, 0, 1)
        val before = buildDepthBoard(team)

        val impact = buildOfferImpact(
            team = team,
            player = player,
            kind = OfferImpactKind.RETENTION,
            proposedStatus = RosterStatus.SCHOLARSHIP,
            proposedNil = 0,
            years = 2,
        )

        val rosterLine = impact.lines.first { it.label == "Roster" }
        val schollyLine = impact.lines.first { it.label == "Scholarships" }
        val pwoLine = impact.lines.first { it.label == "PWOs" }
        val depthLine = impact.lines.first { it.label.endsWith("depth") }

        assertFalse(rosterLine.changed)
        assertFalse(depthLine.changed)
        assertEquals("${before.schollyUsed + 1}/${before.schollyCap}", schollyLine.after)
        assertEquals((before.pwoCount - 1).toString(), pwoLine.after)
        assertNull(impact.blockedReason)
    }

    @Test
    fun portalAdd_blockedWhenScholarshipCapReached() {
        fillScholarshipsToCap(team)
        assertFalse(team.canAwardScholarship())

        val impact = buildOfferImpact(
            team = team,
            player = newRecruit("LB", 70),
            kind = OfferImpactKind.PORTAL_OR_HS,
            proposedStatus = RosterStatus.SCHOLARSHIP,
            proposedNil = 0,
            years = 2,
        )

        assertNotNull(impact.blockedReason)
        assertTrue(impact.blockedReason!!.contains("scholarship", ignoreCase = true))
    }

    private fun newRecruit(position: String, ovr: Int): Player {
        val p = Player()
        p.name = "Test Recruit"
        p.position = position
        p.year = 1
        p.ratOvr = ovr
        p.rosterStatus = RosterStatus.SCHOLARSHIP
        return p
    }

    private fun fillScholarshipsToCap(t: Team) {
        for (p in t.allPlayers) {
            if (t.canAwardScholarship()) {
                if (p.rosterStatus == null || !p.rosterStatus.usesScholarship()) {
                    p.applyOffer(RosterStatus.SCHOLARSHIP, 0, 1)
                }
            }
        }
        // If still under cap (roster smaller than 85), pad with temporary scholarship markers
        // by promoting any remaining non-scholly; if still short, leave as-is and force via
        // cloning into a position list is unnecessary for most league teams (85+ scholly-capable).
        var guard = 0
        while (t.canAwardScholarship() && guard < 200) {
            val filler = Player()
            filler.name = "Pad$guard"
            filler.position = "WR"
            filler.year = 1
            filler.ratOvr = 60
            filler.applyOffer(RosterStatus.SCHOLARSHIP, 0, 1)
            t.teamWRs.add(filler)
            guard++
        }
        assertFalse("expected scholarship cap to be full", t.canAwardScholarship())
    }
}
