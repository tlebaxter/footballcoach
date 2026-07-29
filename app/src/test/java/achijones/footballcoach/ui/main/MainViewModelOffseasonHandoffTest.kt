package achijones.footballcoach.ui.main

import CFBsimPack.GameSession
import CFBsimPack.OffseasonSession
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelOffseasonHandoffTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
    }

    @Test
    fun doneRetentionAdvancesToPortalAndNavigatesTalentHub() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.RETENTION)
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)

        GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_RETENTION)
        vm.onScreenEntered()

        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase)
        assertTrue(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToSchedule)
        assertTrue(File(app.filesDir, "saveLeaguePortal.cfb").exists())
    }

    @Test
    fun doneTransferPortalAdvancesToScheduleAndNavigatesSchedule() {
        val (league, off) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        off.buildTransferPortal()
        // updateLeagueHistory() adds a year; saveLeague requires a matching POTY line.
        league.heismanHistory.add("QB Test [Sr], XXX (0-0)")
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)

        GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_TRANSFER_PORTAL)
        vm.onScreenEntered()

        assertEquals(OffseasonSession.Phase.SCHEDULE, OffseasonSession.phase)
        assertTrue(vm.uiState.value.navigateToSchedule)
        assertFalse(vm.uiState.value.navigateToTalentHub)
        assertTrue(File(app.filesDir, "saveLeagueSchedule.cfb").exists())
    }

    @Test
    fun doneScheduleOpensHsRecruitingWhenNotYearOne() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        GameSession.setNeedsOocScheduling(false)
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)

        GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_SCHEDULE)
        vm.onScreenEntered()

        assertEquals(OffseasonSession.Phase.HS, OffseasonSession.phase)
        assertTrue(OffseasonSession.ready())
        assertTrue(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToSchedule)
        assertTrue(File(app.filesDir, "saveLeagueRecruiting.cfb").exists())
    }

    @Test
    fun doneScheduleYearOneClearsOffseasonAndStaysOnMain() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        GameSession.setNeedsOocScheduling(true)
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)

        GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_SCHEDULE)
        vm.onScreenEntered()

        assertFalse(OffseasonSession.ready())
        assertFalse(GameSession.needsOocScheduling())
        assertFalse(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToSchedule)
    }

    @Test
    fun doneRecruitingAppliesBudgetAndClearsOffseason() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.HS)
        league.userTeam.recruitMoney = 111_111
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)

        GameSession.setPendingRemainingBudget(222_222)
        GameSession.setPendingOffseasonResult(GameSession.OffseasonResult.DONE_RECRUITING)
        vm.onScreenEntered()

        assertFalse(OffseasonSession.ready())
        assertFalse(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToSchedule)
        // aiSignHsClass may spend money; budget is applied before signing.
        assertTrue(GameSession.hasLeague())
    }

    @Test
    fun openTalentHubClearsStayingFlagAndNavigates() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        GameSession.setStayingOnMainDuringOffseason(true)
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)
        assertTrue(GameSession.isStayingOnMainDuringOffseason())

        vm.openTalentHub()

        assertFalse(GameSession.isStayingOnMainDuringOffseason())
        assertTrue(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.showReturnToTalentHub)
    }

    @Test
    fun loadedInOffseasonScheduleRoutesToSchedule() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        league.loadedInOffseason = true
        GameSession.setLeague(league)

        val vm = MainViewModel(app)

        assertTrue(vm.uiState.value.navigateToSchedule)
        assertFalse(vm.uiState.value.navigateToTalentHub)
    }

    @Test
    fun loadedInOffseasonPortalRoutesToTalentHub() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        league.loadedInOffseason = true
        GameSession.setLeague(league)

        val vm = MainViewModel(app)

        assertTrue(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToSchedule)
    }

    private fun consumeOutboundNav(vm: MainViewModel) {
        if (vm.uiState.value.navigateToTalentHub) {
            vm.consumeNavigateToTalentHub()
        }
        if (vm.uiState.value.navigateToSchedule) {
            vm.consumeNavigateToSchedule()
        }
    }
}
