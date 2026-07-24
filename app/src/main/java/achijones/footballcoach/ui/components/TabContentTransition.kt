package achijones.footballcoach.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex

private const val EnterMs = 260
private const val ExitMs = 180

private const val MainEnterMs = 280
private const val MainExitMs = 200

/**
 * Directional fade + short slide for tab / segment content changes.
 * Keeps motion quick and restrained (no bounce, small travel).
 */
fun <S : Comparable<S>> tabContentTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val forward = targetState > initialState
    val enterSlide = if (forward) 28 else -28
    val exitSlide = if (forward) -18 else 18
    (
        fadeIn(animationSpec = tween(EnterMs)) +
            slideInHorizontally(animationSpec = tween(EnterMs)) { enterSlide }
        ) togetherWith (
        fadeOut(animationSpec = tween(ExitMs)) +
            slideOutHorizontally(animationSpec = tween(ExitMs)) { exitSlide }
        )
}

/**
 * Stronger directional fade + slide for bottom-nav main tabs.
 * Enter travels ~width/10; exit ~width/14.
 */
fun <S : Comparable<S>> mainTabContentTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val forward = targetState > initialState
    (
        fadeIn(animationSpec = tween(MainEnterMs, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(MainEnterMs, easing = FastOutSlowInEasing)) {
                if (forward) it / 10 else -it / 10
            }
        togetherWith (
            fadeOut(animationSpec = tween(MainExitMs, easing = FastOutSlowInEasing)) +
                slideOutHorizontally(animationSpec = tween(MainExitMs, easing = FastOutSlowInEasing)) {
                    if (forward) -it / 14 else it / 14
                }
            )
        ).using(SizeTransform(clip = false))
}
@Composable
fun <S : Comparable<S>> TabContentTransition(
    targetState: S,
    modifier: Modifier = Modifier,
    label: String = "tabContent",
    content: @Composable (S) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = tabContentTransition(),
        label = label,
        content = { content(it) },
    )
}

/**
 * Bottom-nav host that keeps visited tabs composed (scroll state survives)
 * and animates show/hide with [mainTabContentTransition] motion.
 */
@Composable
fun <S : Comparable<S>> MainTabContentHost(
    selectedTab: S,
    modifier: Modifier = Modifier,
    content: @Composable (S) -> Unit,
) {
    val visited = remember { mutableStateListOf(selectedTab) }
    if (selectedTab !in visited) {
        visited.add(selectedTab)
    }

    val transition = updateTransition(targetState = selectedTab, label = "mainTabHost")
    var widthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width.toFloat() },
    ) {
        val enterPx = widthPx / 10f
        val exitPx = widthPx / 14f

        visited.forEach { tab ->
            key(tab) {
                val alpha by transition.animateFloat(
                    transitionSpec = {
                        val duration = if (targetState == tab) MainEnterMs else MainExitMs
                        tween(durationMillis = duration, easing = FastOutSlowInEasing)
                    },
                    label = "mainTabAlpha",
                ) { current -> if (current == tab) 1f else 0f }

                val offsetX by transition.animateFloat(
                    transitionSpec = {
                        val duration = if (targetState == tab) MainEnterMs else MainExitMs
                        tween(durationMillis = duration, easing = FastOutSlowInEasing)
                    },
                    label = "mainTabOffset",
                ) { current ->
                    when {
                        current == tab -> 0f
                        tab < current -> -exitPx
                        else -> enterPx
                    }
                }

                val selected = selectedTab == tab
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(if (selected) 1f else 0f)
                        .graphicsLayer {
                            this.alpha = alpha
                            translationX = offsetX
                        },
                ) {
                    content(tab)
                }
            }
        }
    }
}
