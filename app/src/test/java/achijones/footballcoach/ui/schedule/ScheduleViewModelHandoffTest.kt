package achijones.footballcoach.ui.schedule

import CFBsimPack.GameSession
import CFBsimPack.OffseasonSession
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScheduleViewModelHandoffTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
    }

    @Test
    fun confirmDonePreviewSetsDoneScheduleAndNavigatesMain() {
        LeagueFixtures.beginOffseason(phase = OffseasonSession.Phase.SCHEDULE)
        val vm = ScheduleViewModel(app)

        vm.confirmDonePreview()

        assertEquals(
            GameSession.OffseasonResult.DONE_SCHEDULE,
            GameSession.getPendingOffseasonResult(),
        )
        assertTrue(vm.uiState.value.navigateToMain)
        assertTrue(OffseasonSession.ready())
    }
}
