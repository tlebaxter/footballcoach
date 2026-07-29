package achijones.footballcoach.ui.talenthub

import CFBsimPack.GameSession
import CFBsimPack.OffseasonSession
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TalentHubViewModelHandoffTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        LeagueFixtures.clearSessions()
    }

    @Test
    fun portalPrimaryAppliesTransitionAndNavigatesSchedule() {
        val (league, off) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        off.buildTransferPortal()
        league.heismanHistory.add("QB Test [Sr], XXX (0-0)")
        val vm = TalentHubViewModel(app)

        vm.onPrimary()
        waitUntil { vm.uiState.value.navigateToSchedule || vm.uiState.value.message != null }

        assertEquals(OffseasonSession.Phase.SCHEDULE, OffseasonSession.phase)
        assertTrue(vm.uiState.value.navigateToSchedule)
        assertFalse(vm.uiState.value.navigateToMain)
    }

    @Test
    fun hsPrimaryFinishesRecruitingAndNavigatesMain() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.HS)
        val vm = TalentHubViewModel(app)

        vm.onPrimary()
        waitUntil { vm.uiState.value.navigateToMain || vm.uiState.value.message != null }

        assertFalse(OffseasonSession.ready())
        assertTrue(vm.uiState.value.navigateToMain)
    }

    @Test
    fun requestBackToMainKeepsSessionAndSetsStayingFlag() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        val vm = TalentHubViewModel(app)
        assertFalse(GameSession.isStayingOnMainDuringOffseason())

        vm.requestBackToMain()

        assertTrue(GameSession.isStayingOnMainDuringOffseason())
        assertTrue(OffseasonSession.ready())
        assertTrue(vm.uiState.value.navigateToMain)
    }

    @Test
    fun retentionPrimaryAppliesPortalAndStaysOnHub() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.RETENTION)
        val vm = TalentHubViewModel(app)

        vm.onPrimary()
        waitUntil {
            OffseasonSession.phase == OffseasonSession.Phase.PORTAL &&
                vm.uiState.value.selectedTab == HubTab.PORTAL
        }

        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase)
        assertEquals(HubTab.PORTAL, vm.uiState.value.selectedTab)
        assertFalse(vm.uiState.value.navigateToMain)
        assertTrue(OffseasonSession.offseason.transferPortal.isNotEmpty())
    }

    private fun waitUntil(predicate: () -> Boolean) {
        var waited = 0
        while (!predicate() && waited < 100) {
            Thread.sleep(20)
            waited++
        }
        assertTrue(predicate())
    }
}
