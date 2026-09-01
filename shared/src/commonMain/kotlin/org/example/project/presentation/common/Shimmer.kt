package org.example.project.presentation.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

private const val SHIMMER_ANIMATION_DURATION_MS = 1200
private const val SHIMMER_TRAVEL_DISTANCE = 1000f
private const val SHIMMER_BAND_WIDTH = 500f

/**
 * Skeleton-loader shimmer: an infinitely looping diagonal gradient sweep,
 * meant to sit on a plain-colored Box standing in for not-yet-loaded content.
 *
 * Uses `Modifier.composed` (rather than a Modifier.Node) deliberately: the
 * effect needs `@Composable` access to `MaterialTheme.colorScheme` and a
 * per-instance `rememberInfiniteTransition`, which is exactly the case
 * `composed` remains the correct, supported tool for.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = SHIMMER_TRAVEL_DISTANCE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.3f),
            baseColor.copy(alpha = 0.9f),
            baseColor.copy(alpha = 0.3f),
        ),
        // Both start and end move along the same diagonal (x == y), which is
        // what makes the sweep read as diagonal rather than horizontal/vertical.
        start = Offset(translate - SHIMMER_BAND_WIDTH, translate - SHIMMER_BAND_WIDTH),
        end = Offset(translate, translate),
    )

    background(brush)
}
