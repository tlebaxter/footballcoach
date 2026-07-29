package achijones.footballcoach.ui.main

import CFBsimPack.Game
import CFBsimPack.GameSession
import achijones.footballcoach.save.SaveRepository
import achijones.footballcoach.save.SlotStatus
import achijones.footballcoach.testing.LeagueFixtures
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
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
    private val repo = SaveRepository.get(app)

    @After
    fun tearDown() = runBlocking {
        LeagueFixtures.clearSessions()
        repo.delete(0)
        repo.setLastActiveSlot(null)
    }

    @Test
    fun coachFinishWithActiveSlotAutosavesAndSnackbars() = runBlocking {
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
        val slot = repo.listSlots()[0]
        assertEquals(SlotStatus.OK, slot.status)
        assertFalse(GameSession.consumePendingCoachResultSave())
    }

    @Test
    fun coachFinishWithoutSlotPromptsCareerSave() = runBlocking {
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
        assertEquals(SlotStatus.EMPTY, repo.listSlots()[0].status)
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
