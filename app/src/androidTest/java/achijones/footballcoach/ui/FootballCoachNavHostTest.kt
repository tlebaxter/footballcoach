package achijones.footballcoach.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import achijones.footballcoach.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FootballCoachNavHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startsAtHome() {
        val nav = testNav()
        setStubNavHost(nav)
        assertEquals(Routes.HOME, nav.currentDestination?.route)
    }

    @Test
    fun mainCanReachTalentHubScheduleAndCoach() {
        val nav = testNav()
        setStubNavHost(nav)

        composeRule.onNodeWithText("Go Main").performClick()
        assertEquals(Routes.MAIN, nav.currentDestination?.route)

        composeRule.onNodeWithText("Go Talent Hub").performClick()
        assertEquals(Routes.TALENT_HUB, nav.currentDestination?.route)

        composeRule.onNodeWithText("Hub to Main").performClick()
        assertEquals(Routes.MAIN, nav.currentDestination?.route)
        assertFalse(nav.currentBackStack.value.any { it.destination.route == Routes.TALENT_HUB })

        composeRule.onNodeWithText("Go Schedule").performClick()
        assertEquals(Routes.SCHEDULE, nav.currentDestination?.route)

        composeRule.onNodeWithText("Schedule to Main").performClick()
        assertEquals(Routes.MAIN, nav.currentDestination?.route)
        assertFalse(nav.currentBackStack.value.any { it.destination.route == Routes.SCHEDULE })

        composeRule.onNodeWithText("Go Coach").performClick()
        assertEquals(Routes.COACH_GAME, nav.currentDestination?.route)

        composeRule.onNodeWithText("Finish Coach").performClick()
        assertEquals(Routes.MAIN, nav.currentDestination?.route)
        assertFalse(nav.currentBackStack.value.any { it.destination.route == Routes.COACH_GAME })
    }

    @Test
    fun talentHubAndSchedulePopInclusiveLeavingSingleMain() {
        val nav = testNav()
        setStubNavHost(nav)

        composeRule.onNodeWithText("Go Main").performClick()
        composeRule.onNodeWithText("Go Talent Hub").performClick()
        composeRule.onNodeWithText("Hub to Main").performClick()
        composeRule.onNodeWithText("Go Schedule").performClick()
        composeRule.onNodeWithText("Schedule to Main").performClick()

        assertEquals(Routes.MAIN, nav.currentDestination?.route)
        val mainEntries = nav.currentBackStack.value.count { it.destination.route == Routes.MAIN }
        assertEquals(1, mainEntries)
    }

    private fun testNav(): TestNavHostController {
        val nav = TestNavHostController(ApplicationProvider.getApplicationContext())
        nav.navigatorProvider.addNavigator(ComposeNavigator())
        return nav
    }

    private fun setStubNavHost(nav: TestNavHostController) {
        composeRule.setContent {
            FootballCoachNavHost(
                navController = nav,
                home = { onNavigateToMain ->
                    Column {
                        Button(onClick = onNavigateToMain) { Text("Go Main") }
                    }
                },
                main = { _, onNavigateTalentHub, onNavigateCoach, onNavigateSchedule ->
                    Column {
                        Button(onClick = onNavigateTalentHub) { Text("Go Talent Hub") }
                        Button(onClick = onNavigateCoach) { Text("Go Coach") }
                        Button(onClick = onNavigateSchedule) { Text("Go Schedule") }
                    }
                },
                coachGame = { onFinished ->
                    Column {
                        Button(onClick = onFinished) { Text("Finish Coach") }
                    }
                },
                schedule = { onNavigateToMain ->
                    Column {
                        Button(onClick = onNavigateToMain) { Text("Schedule to Main") }
                    }
                },
                talentHub = { onNavigateToMain, _ ->
                    Column {
                        Button(onClick = onNavigateToMain) { Text("Hub to Main") }
                    }
                },
            )
        }
    }
}
