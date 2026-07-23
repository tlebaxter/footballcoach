package achijones.footballcoach.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val EnterMs = 260
private const val ExitMs = 180

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
