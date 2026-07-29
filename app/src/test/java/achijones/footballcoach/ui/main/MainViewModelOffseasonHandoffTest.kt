package achijones.footballcoach.ui.main

import CFBsimPack.GameSession
import CFBsimPack.OffseasonSession
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
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

    @Test
    fun stayingOnMainSyncsWithoutMutatingPhase() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        GameSession.setStayingOnMainDuringOffseason(true)
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        consumeOutboundNav(vm)

        vm.onScreenEntered()

        assertTrue(OffseasonSession.ready())
        assertTrue(OffseasonSession.phase == OffseasonSession.Phase.PORTAL)
        assertFalse(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToSchedule)
        assertTrue(vm.uiState.value.ready)
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
