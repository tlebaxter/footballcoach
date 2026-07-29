package achijones.footballcoach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import achijones.footballcoach.ui.coach.CoachGameScreen
import achijones.footballcoach.ui.components.HideSystemBarsWhileVisible
import achijones.footballcoach.ui.home.HomeScreen
import achijones.footballcoach.ui.main.MainScreen
import achijones.footballcoach.ui.schedule.ScheduleScreen
import achijones.footballcoach.ui.talenthub.TalentHubScreen

@Composable
fun FootballCoachApp() {
    val navController = rememberNavController()
    HideSystemBarsWhileVisible()

    FootballCoachNavHost(
        navController = navController,
        home = { onNavigateToMain ->
            HomeScreen(onNavigateToMain = onNavigateToMain)
        },
        main = { onNavigateHome, onNavigateTalentHub, onNavigateCoach, onNavigateSchedule ->
            MainScreen(
                onNavigateHome = onNavigateHome,
                onNavigateTalentHub = onNavigateTalentHub,
                onNavigateCoach = onNavigateCoach,
                onNavigateSchedule = onNavigateSchedule,
            )
        },
        coachGame = { onFinished ->
            CoachGameScreen(onFinished = onFinished)
        },
        schedule = { onNavigateToMain ->
            ScheduleScreen(onNavigateToMain = onNavigateToMain)
        },
        talentHub = { onNavigateToMain, onNavigateHome ->
            TalentHubScreen(
                onNavigateToMain = onNavigateToMain,
                onNavigateHome = onNavigateHome,
            )
        },
    )
}
