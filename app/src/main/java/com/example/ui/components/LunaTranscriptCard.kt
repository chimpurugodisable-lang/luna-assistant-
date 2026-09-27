package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LunaVoiceState
import com.example.ui.theme.LunaBorderGlow
import com.example.ui.theme.LunaCyan
import com.example.ui.theme.LunaEmerald
import com.example.ui.theme.LunaMoonGold
import com.example.ui.theme.LunaRuby
import com.example.ui.theme.LunaSurfaceElevated
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.LunaTextTertiary
import com.example.ui.theme.LunaViolet

@Composable
fun LunaTranscriptCard(
    state: LunaVoiceState,
    spokenText: String,
    assistantResponse: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("luna_transcript_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LunaSurfaceElevated.copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LunaBorderGlow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // State Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (state) {
                        LunaVoiceState.IDLE -> Color(0xFF64748B)
                        LunaVoiceState.LISTENING_FOR_WAKE_WORD -> LunaCyan
                        LunaVoiceState.WAKE_WORD_DETECTED, LunaVoiceState.LISTENING_FOR_COMMAND -> LunaEmerald
                        LunaVoiceState.THINKING -> LunaViolet
                        LunaVoiceState.EXECUTING -> LunaMoonGold
                        LunaVoiceState.SPEAKING -> LunaCyan
                        LunaVoiceState.ERROR -> LunaRuby
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.label,
                        color = LunaTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("luna_status_text")
                    )
                }

                when (state) {
                    LunaVoiceState.LISTENING_FOR_COMMAND -> Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = LunaEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    LunaVoiceState.SPEAKING -> Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaking",
                        tint = LunaCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    LunaVoiceState.THINKING -> Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Thinking",
                        tint = LunaViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    else -> {}
                }
            }

            // User speech prompt
            AnimatedVisibility(
                visible = spokenText.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "YOU",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LunaCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = spokenText,
                        fontSize = 15.sp,
                        color = LunaTextPrimary,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.testTag("user_spoken_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // LUNA Response
            Text(
                text = "LUNA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LunaViolet,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = assistantResponse,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = LunaTextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("luna_response_text")
            )
        }
    }
}
