package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.LunaVoiceState
import com.example.ui.theme.LunaCyan
import com.example.ui.theme.LunaEmerald
import com.example.ui.theme.LunaMoonGold
import com.example.ui.theme.LunaRuby
import com.example.ui.theme.LunaSpaceDark
import com.example.ui.theme.LunaViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LunaOrbVisualizer(
    state: LunaVoiceState,
    rmsAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "luna_orb_infinite")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Dynamic color palette based on assistant state
    val (primaryGlow, accentGlow) = when (state) {
        LunaVoiceState.IDLE -> Pair(Color(0xFF3949AB), Color(0xFF1E293B))
        LunaVoiceState.LISTENING_FOR_WAKE_WORD -> Pair(LunaCyan, LunaViolet)
        LunaVoiceState.WAKE_WORD_DETECTED -> Pair(LunaMoonGold, LunaCyan)
        LunaVoiceState.LISTENING_FOR_COMMAND -> Pair(LunaCyan, LunaEmerald)
        LunaVoiceState.THINKING -> Pair(LunaViolet, LunaMoonGold)
        LunaVoiceState.EXECUTING -> Pair(LunaEmerald, LunaCyan)
        LunaVoiceState.SPEAKING -> Pair(LunaCyan, LunaViolet)
        LunaVoiceState.ERROR -> Pair(LunaRuby, Color(0xFFFF8A80))
    }

    // Audio-reactive scale boost
    val audioBoost = (rmsAmplitude.coerceIn(0f, 15f) / 15f) * 0.25f

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("luna_orb_visualizer")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 130.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 3.4f
            val dynamicRadius = baseRadius * (pulseScale + audioBoost)

            // 1. Outer ambient aura rings
            val outerRadius = dynamicRadius * 1.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // 2. Ripple waves for active listening / speaking
            if (state == LunaVoiceState.LISTENING_FOR_COMMAND || state == LunaVoiceState.SPEAKING || state == LunaVoiceState.WAKE_WORD_DETECTED) {
                for (i in 1..3) {
                    val waveRadius = dynamicRadius + (i * 22f * (pulseScale + audioBoost))
                    drawCircle(
                        color = primaryGlow.copy(alpha = 0.35f / i),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // 3. Rotating celestial particle ring when thinking
            if (state == LunaVoiceState.THINKING) {
                val orbitRadius = dynamicRadius * 1.25f
                for (angle in 0 until 360 step 45) {
                    val rad = Math.toRadians((angle + rotationAngle).toDouble())
                    val dotPos = Offset(
                        center.x + (orbitRadius * cos(rad)).toFloat(),
                        center.y + (orbitRadius * sin(rad)).toFloat()
                    )
                    drawCircle(
                        color = accentGlow.copy(alpha = 0.8f),
                        radius = 4f,
                        center = dotPos
                    )
                }
            }

            // 4. Main Lunar Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.95f),
                        accentGlow.copy(alpha = 0.85f),
                        LunaSpaceDark
                    ),
                    center = Offset(center.x - dynamicRadius * 0.2f, center.y - dynamicRadius * 0.25f),
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // 5. Stylized Lunar Crescent Halo
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = dynamicRadius * 0.92f,
                center = Offset(center.x - 3f, center.y - 3f),
                style = Stroke(width = 2f)
            )
        }
    }
}
