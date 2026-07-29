package achijones.footballcoach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import achijones.footballcoach.ui.navigation.Routes

/**
 * Shared navigation graph. Production wires real screens; tests pass stub destinations
 * so popUpTo / launchSingleTop policy can be asserted without bootstrapping a league.
 */
@Composable
fun FootballCoachNavHost(
    navController: NavHostController,
    startDestination: String = Routes.HOME,
    home: @Composable (onNavigateToMain: () -> Unit) -> Unit,
    main: @Composable (
        onNavigateHome: () -> Unit,
        onNavigateTalentHub: () -> Unit,
        onNavigateCoach: () -> Unit,
        onNavigateSchedule: () -> Unit,
    ) -> Unit,
    coachGame: @Composable (onFinished: () -> Unit) -> Unit,
    schedule: @Composable (onNavigateToMain: () -> Unit) -> Unit,
    talentHub: @Composable (
        onNavigateToMain: () -> Unit,
        onNavigateHome: () -> Unit,
    ) -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            home({
                navController.navigate(Routes.MAIN) {
                    launchSingleTop = true
                }
            })
        }
        composable(Routes.MAIN) {
            main(
                {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                {
                    navController.navigate(Routes.TALENT_HUB) {
                        launchSingleTop = true
                    }
                },
                {
                    navController.navigate(Routes.COACH_GAME) {
                        launchSingleTop = true
                    }
                },
                {
                    navController.navigate(Routes.SCHEDULE) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.COACH_GAME) {
            coachGame({
                navController.popBackStack()
            })
        }
        composable(Routes.SCHEDULE) {
            schedule({
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.SCHEDULE) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable(Routes.TALENT_HUB) {
            talentHub(
                {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.TALENT_HUB) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
