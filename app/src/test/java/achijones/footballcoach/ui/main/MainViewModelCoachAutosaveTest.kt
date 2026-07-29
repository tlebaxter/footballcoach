package achijones.footballcoach.ui.main

import CFBsimPack.Game
import CFBsimPack.GameSession
import achijones.footballcoach.testing.LeagueFixtures
import achijones.footballcoach.ui.util.SaveSlots
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelCoachAutosaveTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
        SaveSlots.file(app, 0).delete()
    }

    @Test
    fun coachFinishWithActiveSlotAutosavesAndSnackbars() {
        val league = LeagueFixtures.createLeagueWithUser()
        GameSession.setLeague(league)
        GameSession.setActiveSaveSlot(0)
        val vm = MainViewModel(app)
        vm.consumeSnackbar()

        val game = Game(league.userTeam, league.teamList[1], "Coached")
        game.hasPlayed = true
        game.homeScore = 28
        game.awayScore = 14
        GameSession.finishCoachGame(game)

        vm.onScreenEntered()

        assertEquals("Game saved · 28-14", vm.uiState.value.snackbarMessage)
        assertTrue(SaveSlots.file(app, 0).exists())
        assertFalse(GameSession.consumePendingCoachResultSave())
    }

    @Test
    fun coachFinishWithoutSlotPromptsCareerSave() {
        val league = LeagueFixtures.createLeagueWithUser()
        GameSession.setLeague(league)
        val vm = MainViewModel(app)
        vm.consumeSnackbar()

        val game = Game(league.userTeam, league.teamList[1], "Coached")
        game.hasPlayed = true
        game.homeScore = 21
        game.awayScore = 17
        GameSession.finishCoachGame(game)

        vm.onScreenEntered()

        assertEquals(
            "Result applied · 21-17 — save your career to keep it",
            vm.uiState.value.snackbarMessage,
        )
        assertFalse(SaveSlots.file(app, 0).exists())
    }

    @Test
    fun coachAutosaveIsOneShot() {
        val league = LeagueFixtures.createLeagueWithUser()
        GameSession.setLeague(league)
        GameSession.setActiveSaveSlot(0)
        val vm = MainViewModel(app)
        vm.consumeSnackbar()

        val game = Game(league.userTeam, league.teamList[1], "Coached")
        game.hasPlayed = true
        game.homeScore = 10
        game.awayScore = 3
        GameSession.finishCoachGame(game)

        vm.onScreenEntered()
        assertEquals("Game saved · 10-3", vm.uiState.value.snackbarMessage)
        vm.consumeSnackbar()

        vm.onScreenEntered()
        assertNull(vm.uiState.value.snackbarMessage)
    }
}
