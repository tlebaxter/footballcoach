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
    fun portalPrimarySetsPendingAndNavigatesMain() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.PORTAL)
        val vm = TalentHubViewModel(app)

        vm.onPrimary()

        assertEquals(
            GameSession.OffseasonResult.DONE_TRANSFER_PORTAL,
            GameSession.getPendingOffseasonResult(),
        )
        assertTrue(vm.uiState.value.navigateToMain)
    }

    @Test
    fun hsPrimarySetsBudgetPendingAndNavigatesMain() {
        val (league, _) = LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.HS)
        league.userTeam.recruitMoney = 333_000
        val vm = TalentHubViewModel(app)

        vm.onPrimary()

        assertEquals(
            GameSession.OffseasonResult.DONE_RECRUITING,
            GameSession.getPendingOffseasonResult(),
        )
        assertEquals(333_000, GameSession.getPendingRemainingBudget())
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
    fun retentionPrimaryAppliesAndSetsDoneRetention() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.RETENTION)
        val vm = TalentHubViewModel(app)

        vm.onPrimary()
        // approveRetention uses Dispatchers.Default; poll until it resumes on Main.
        var waited = 0
        while (!vm.uiState.value.navigateToMain && waited < 100) {
            Thread.sleep(20)
            waited++
        }

        assertTrue(vm.uiState.value.navigateToMain)
        assertEquals(
            GameSession.OffseasonResult.DONE_RETENTION,
            GameSession.getPendingOffseasonResult(),
        )
        assertEquals(OffseasonSession.Phase.PORTAL, OffseasonSession.phase)
    }
}
