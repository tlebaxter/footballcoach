package achijones.footballcoach.ui.schedule

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
class ScheduleViewModelHandoffTest {

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
    fun finishSchedulingOpensHsAndNavigatesTalentHub() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        GameSession.setNeedsOocScheduling(false)
        val vm = ScheduleViewModel(app)

        vm.onPrimary()
        if (vm.uiState.value.showDonePreview) {
            vm.confirmDonePreview()
        }

        var waited = 0
        while (!vm.uiState.value.navigateToTalentHub &&
            !vm.uiState.value.navigateToMain &&
            waited < 100
        ) {
            Thread.sleep(20)
            waited++
        }

        assertEquals(OffseasonSession.Phase.HS, OffseasonSession.phase)
        assertTrue(vm.uiState.value.navigateToTalentHub)
        assertFalse(vm.uiState.value.navigateToMain)
    }
}
