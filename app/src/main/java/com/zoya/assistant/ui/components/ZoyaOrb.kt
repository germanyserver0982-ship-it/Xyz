package com.zoya.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.zoya.assistant.ai.ZoyaState
import com.zoya.assistant.ui.theme.ZoyaCyan
import com.zoya.assistant.ui.theme.ZoyaPink
import com.zoya.assistant.ui.theme.ZoyaViolet
import kotlin.math.sin

@Composable
fun ZoyaOrb(
    state: ZoyaState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "zoya-orb")

    // Idle: slow breathing glow.
    val breathing by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathing"
    )

    // Thinking: pulsing neon ring rotation + scale pulse.
    val ringRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ring-rotation"
    )
    val ringPulse by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring-pulse"
    )

    // Continuous phase for waveform drawing, independent of amplitude updates.
    val wavePhase by infinite.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "wave-phase"
    )

    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "amplitude"
    )

    val gradient = Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink, ZoyaCyan, ZoyaViolet))

    Canvas(
        modifier = modifier
            .size(220.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 2 * 0.55f

        when (state) {
            ZoyaState.IDLE, ZoyaState.CONNECTING -> {
                val r = baseRadius * breathing
                drawCircle(brush = gradient, radius = r, center = center, alpha = 0.35f)
                drawCircle(brush = gradient, radius = r * 0.7f, center = center, alpha = 0.6f)
            }

            ZoyaState.LISTENING -> {
                drawCircle(brush = gradient, radius = baseRadius, center = center, alpha = 0.55f)
                // Waveform ring responding to mic amplitude.
                val points = 64
                val path = androidx.compose.ui.graphics.Path()
                for (i in 0..points) {
                    val angle = (i.toFloat() / points) * 2 * Math.PI
                    val wobble = 1f + animatedAmplitude * 0.5f * sin(angle * 6 + wavePhase).toFloat()
                    val r = baseRadius * (0.9f + 0.25f * wobble)
                    val x = center.x + (r * kotlin.math.cos(angle)).toFloat()
                    val y = center.y + (r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, brush = gradient, style = Stroke(width = 5f))
            }

            ZoyaState.THINKING -> {
                drawCircle(brush = gradient, radius = baseRadius * 0.75f, center = center, alpha = 0.4f)
                rotate(degrees = ringRotation, pivot = center) {
                    drawCircle(
                        brush = gradient,
                        radius = baseRadius * ringPulse,
                        center = center,
                        style = Stroke(width = 10f)
                    )
                }
            }

            ZoyaState.SPEAKING -> {
                drawCircle(brush = gradient, radius = baseRadius, center = center, alpha = 0.5f)
                // Multiple concentric waves reacting to output amplitude.
                for (ring in 0..2) {
                    val phase = wavePhase + ring * 1.2f
                    val r = baseRadius * (0.65f + ring * 0.15f) * (1f + animatedAmplitude * 0.35f * sin(phase))
                    drawCircle(
                        brush = gradient,
                        radius = r,
                        center = center,
                        alpha = 0.5f - ring * 0.12f,
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        // Solid core so the orb always reads as a tappable button regardless of state.
        drawCircle(color = Color.White, radius = baseRadius * 0.18f, center = center, alpha = 0.9f)
    }
}
