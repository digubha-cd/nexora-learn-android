package com.example.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NEXORA LEARN Central Animation & Motion System
 * Provides fluid spring physics, tactile press scale feedback, staggered list entrance,
 * and animated rolling text counters.
 */
object NexoraMotion {

    // Spring physics specifications
    val FluidSpringFloat: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SnappySpringFloat: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SoftSpringFloat: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Standard durations
    const val DURATION_FAST = 200
    const val DURATION_MEDIUM = 350
    const val DURATION_SLOW = 500

    /**
     * Shared-axis Horizontal Slide + Fade transition for bottom navigation tab switching
     */
    fun tabSlideTransition(isForward: Boolean): ContentTransform {
        return slideInHorizontally(
            animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing),
            initialOffsetX = { fullWidth -> if (isForward) fullWidth / 3 else -fullWidth / 3 }
        ) + fadeIn(
            animationSpec = tween(DURATION_MEDIUM)
        ) togetherWith slideOutHorizontally(
            animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing),
            targetOffsetX = { fullWidth -> if (isForward) -fullWidth / 3 else fullWidth / 3 }
        ) + fadeOut(
            animationSpec = tween(DURATION_MEDIUM)
        )
    }

    /**
     * Vertical Slide-up + Fade transition for dialogs, modals, and detail sheets
     */
    fun modalSlideTransition(): ContentTransform {
        return slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { fullHeight -> fullHeight / 2 }
        ) + fadeIn(
            animationSpec = tween(DURATION_MEDIUM)
        ) togetherWith slideOutVertically(
            animationSpec = tween(DURATION_FAST, easing = FastOutSlowInEasing),
            targetOffsetY = { fullHeight -> fullHeight / 2 }
        ) + fadeOut(
            animationSpec = tween(DURATION_FAST)
        )
    }
}

/**
 * Modifier to add tactile bounce/press scale feedback to any clickable element.
 */
fun Modifier.bounceClick(
    scaleDownRatio: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDownRatio else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceClickScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Modifier for staggered entrance animations (fade + slide-up) for list items and cards.
 */
fun Modifier.staggeredEntrance(
    index: Int = 0,
    baseDelayMs: Int = 50,
    slideOffsetDp: Float = 40f
): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(slideOffsetDp) }

    LaunchedEffect(Unit) {
        delay((index * baseDelayMs).toLong())
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay((index * baseDelayMs).toLong())
        offsetYAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    this.graphicsLayer {
        alpha = alphaAnim.value
        translationY = offsetYAnim.value
    }
}

/**
 * Modifier for subtle pop-in entrance animation (spring scale + fade).
 */
fun Modifier.popIn(
    delayMs: Long = 0L,
    initialScale: Float = 0.85f
): Modifier = composed {
    val scaleAnim = remember { Animatable(initialScale) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    this.graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
        alpha = alphaAnim.value
    }
}

/**
 * Animated Rolling Counter Text component for smooth numerical rollups.
 */
@Composable
fun AnimatedCounterText(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    prefix: String = "",
    suffix: String = ""
) {
    val countString = count.toString()

    Row(modifier = modifier) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = style)
        }

        countString.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "CounterDigit_$index"
            ) { digit ->
                Text(text = digit.toString(), style = style)
            }
        }

        if (suffix.isNotEmpty()) {
            Text(text = suffix, style = style)
        }
    }
}
