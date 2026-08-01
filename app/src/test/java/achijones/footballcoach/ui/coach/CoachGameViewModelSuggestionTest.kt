package achijones.footballcoach.ui.coach

import CFBsimPack.Game
import CFBsimPack.GameSession
import CFBsimPack.engine.Playbook
import achijones.footballcoach.testing.LeagueFixtures
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CoachGameViewModelSuggestionTest {

    @After
    fun tearDown() {
        LeagueFixtures.clearSessions()
    }

    @Test
    fun simPlayRefreshesSuggestionAfterKickoff() {
        val league = LeagueFixtures.createLeagueWithUser()
        val user = league.userTeam
        val opp = league.teamList.first { it !== user }
        val game = Game(user, opp, "Coach")
        // Force user (home) to win the toss so confirmCoinToss is available.
        game.setRandom(object : Random(1L) {
            private var tossDrawn = false
            override fun nextBoolean(): Boolean {
                if (!tossDrawn) {
                    tossDrawn = true
                    return true
                }
                return super.nextBoolean()
            }
        })
        GameSession.setActiveCoachGame(game)

        val vm = CoachGameViewModel()
        assertTrue(vm.uiState.value.showCoinToss)
        // Defer → user kicks; Call Plays card selects kickoff.
        vm.confirmCoinToss(receive = false, defendLeft = true)

        val afterToss = vm.uiState.value
        val tossSit = checkNotNull(afterToss.situation)
        assertTrue(tossSit.pendingKickoff)
        assertTrue(tossSit.userOnOffense)
        assertEqualsKickoff(afterToss.selectedOffense.id)

        vm.simPlay()

        val after = vm.uiState.value
        val sit = checkNotNull(after.situation)
        assertFalse(sit.gameOver)
        assertFalse(sit.userChoosesTry)
        assertFalse(sit.pendingKickoff)

        if (sit.userOnOffense) {
            assertNotEquals("kickoff", after.selectedOffense.id)
        } else {
            assertFalse(
                "Defense suggestion should leave kick-return packages",
                after.selectedDefense.id == "kick_return" ||
                    after.selectedDefense.id == "kick_fair_catch",
            )
            assertFalse(Playbook.isSpecialTeamsDefense(after.selectedDefense))
        }
    }

    @Test
    fun applySuggestionChangesToDifferentPlay() {
        val league = LeagueFixtures.createLeagueWithUser()
        val user = league.userTeam
        val opp = league.teamList.first { it !== user }
        val game = Game(user, opp, "Coach")
        game.setRandom(object : Random(1L) {
            private var tossDrawn = false
            override fun nextBoolean(): Boolean {
                if (!tossDrawn) {
                    tossDrawn = true
                    return true
                }
                return super.nextBoolean()
            }
        })
        GameSession.setActiveCoachGame(game)

        val vm = CoachGameViewModel()
        vm.confirmCoinToss(receive = true, defendLeft = true)
        // Opening kickoff — receive side is on defense.
        if (checkNotNull(vm.uiState.value.situation).pendingKickoff) {
            vm.simPlay()
        }
        val sit = checkNotNull(vm.uiState.value.situation)
        assertFalse(sit.gameOver)
        assertFalse(sit.pendingKickoff)

        if (sit.userOnOffense) {
            val before = vm.uiState.value.selectedOffense.id
            val alts = Playbook.situationalOffense(game.state, true).filter { it.id != before }
            assertTrue("need alternatives to verify re-roll", alts.isNotEmpty())
            vm.applySuggestion(true)
            assertNotEquals(before, vm.uiState.value.selectedOffense.id)
        } else {
            val before = vm.uiState.value.selectedDefense.id
            val alts = Playbook.situationalDefense(game.state).filter { it.id != before }
            assertTrue("need alternatives to verify re-roll", alts.isNotEmpty())
            vm.applySuggestion(false)
            assertNotEquals(before, vm.uiState.value.selectedDefense.id)
        }
    }

    @Test
    fun simPlayPreservesAiCallModeWhenRefreshingSuggestion() {
        val league = LeagueFixtures.createLeagueWithUser()
        val user = league.userTeam
        val opp = league.teamList.first { it !== user }
        val game = Game(user, opp, "Coach")
        game.setRandom(object : Random(1L) {
            private var tossDrawn = false
            override fun nextBoolean(): Boolean {
                if (!tossDrawn) {
                    tossDrawn = true
                    return true
                }
                return super.nextBoolean()
            }
        })
        GameSession.setActiveCoachGame(game)

        val vm = CoachGameViewModel()
        vm.confirmCoinToss(receive = false, defendLeft = true)
        vm.setAiCallMode(true)
        assertTrue(vm.uiState.value.aiCallMode)

        vm.simPlay()

        val after = vm.uiState.value
        val sit = checkNotNull(after.situation)
        if (!sit.gameOver && !sit.userChoosesTry) {
            assertTrue(after.aiCallMode)
        }
    }

    private fun assertEqualsKickoff(id: String) {
        assertTrue("expected kickoff, got $id", id == "kickoff")
    }
}
