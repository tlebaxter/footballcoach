package achijones.footballcoach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import achijones.footballcoach.ui.coach.CoachGameScreen
import achijones.footballcoach.ui.home.HomeScreen
import achijones.footballcoach.ui.main.MainScreen
import achijones.footballcoach.ui.navigation.Routes
import achijones.footballcoach.ui.talenthub.TalentHubScreen
import achijones.footballcoach.ui.tutorial.TutorialScreen

@Composable
fun FootballCoachApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTutorial = {
                    navController.navigate(Routes.TUTORIAL)
                },
            )
        }
        composable(Routes.TUTORIAL) {
            TutorialScreen(onBack = { navController.popBackStack() })
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
            )
        }
        composable(Routes.COACH_GAME) {
            CoachGameScreen(
                onFinished = {
                    navController.popBackStack()
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
