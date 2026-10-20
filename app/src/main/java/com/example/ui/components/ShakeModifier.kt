package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

class ShakeState {
    var shakeTrigger by mutableIntStateOf(0)
        private set

    fun shake() {
        shakeTrigger++
    }
}

@Composable
fun rememberShakeState(): ShakeState {
    return remember { ShakeState() }
}

fun Modifier.shake(shakeTrigger: Int): Modifier = composed {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            animatable.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-12f) at 50
                    12f at 100
                    (-9f) at 150
                    9f at 200
                    (-6f) at 250
                    6f at 300
                    (-3f) at 350
                    0f at 400
                }
            )
        }
    }

    this.graphicsLayer {
        translationX = animatable.value * density
    }
}

fun Modifier.shake(shakeState: ShakeState): Modifier = this.shake(shakeState.shakeTrigger)
