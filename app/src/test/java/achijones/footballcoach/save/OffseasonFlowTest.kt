package achijones.footballcoach.save

import CFBsimPack.GameSession
import CFBsimPack.OffseasonSession
import achijones.footballcoach.testing.LeagueFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffseasonFlowTest {

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
    }

    @Test
    fun finishRetentionBuildsPortal() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.RETENTION)
        val next = OffseasonFlow.finishRetention()
        assertEquals(OffseasonFlow.Next.STAY_TALENT_HUB, next)
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase)
        assertTrue(OffseasonSession.offseason.transferPortal.isNotEmpty())
    }

    @Test
    fun finishPortalAdvancesToSchedule() {
        val (league, off) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        off.buildTransferPortal()
        league.heismanHistory.add("QB Test [Sr], XXX (0-0)")
        val next = OffseasonFlow.finishPortal()
        assertEquals(OffseasonFlow.Next.SCHEDULE, next)
        assertEquals(OffseasonSession.Phase.SCHEDULE, OffseasonSession.phase)
    }

    @Test
    fun finishScheduleOpensHsWhenNotYearOne() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        GameSession.setNeedsOocScheduling(false)
        val next = OffseasonFlow.finishSchedule()
        assertEquals(OffseasonFlow.Next.TALENT_HUB, next)
        assertEquals(OffseasonSession.Phase.HS, OffseasonSession.phase)
        assertTrue(OffseasonSession.ready())
    }

    @Test
    fun finishScheduleYearOneClearsOffseason() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        GameSession.setNeedsOocScheduling(true)
        val next = OffseasonFlow.finishSchedule()
        assertEquals(OffseasonFlow.Next.MAIN, next)
        assertFalse(OffseasonSession.ready())
        assertFalse(GameSession.needsOocScheduling())
    }

    @Test
    fun finishRecruitingClearsOffseason() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.HS)
        league.userTeam.recruitMoney = 111_111
        val next = OffseasonFlow.finishRecruiting(222_222)
        assertEquals(OffseasonFlow.Next.MAIN, next)
        assertFalse(OffseasonSession.ready())
        assertTrue(GameSession.hasLeague())
    }
}
