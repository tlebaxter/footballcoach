package achijones.footballcoach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import achijones.footballcoach.ui.coach.CoachGameScreen
import achijones.footballcoach.ui.components.HideSystemBarsWhileVisible
import achijones.footballcoach.ui.home.HomeScreen
import achijones.footballcoach.ui.main.MainScreen
import achijones.footballcoach.ui.navigation.Routes
import achijones.footballcoach.ui.schedule.ScheduleScreen
import achijones.footballcoach.ui.talenthub.TalentHubScreen

@Composable
fun FootballCoachApp() {
    val navController = rememberNavController()
    HideSystemBarsWhileVisible()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateTalentHub = {
                    navController.navigate(Routes.TALENT_HUB) {
                        launchSingleTop = true
                    }
                },
                onNavigateCoach = {
                    navController.navigate(Routes.COACH_GAME) {
                        launchSingleTop = true
                    }
                },
                onNavigateSchedule = {
                    navController.navigate(Routes.SCHEDULE) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.COACH_GAME) {
            CoachGameScreen(
                onFinished = {
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.SCHEDULE) {
            ScheduleScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SCHEDULE) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.TALENT_HUB) {
            TalentHubScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.TALENT_HUB) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
