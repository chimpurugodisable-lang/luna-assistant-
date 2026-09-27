package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LunaBorderGlow
import com.example.ui.theme.LunaCyan
import com.example.ui.theme.LunaEmerald
import com.example.ui.theme.LunaRuby
import com.example.ui.theme.LunaSurfaceElevated
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.LunaTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LunaDiagnosticsCard(
    isServiceRunning: Boolean,
    onToggleService: (Boolean) -> Unit,
    wakeWordStatus: String,
    hasMicPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasContactsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("luna_diagnostics_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LunaSurfaceElevated.copy(alpha = 0.7f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LunaBorderGlow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with toggle switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HANDS-FREE WAKE WORD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LunaCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isServiceRunning) "Continuous listening enabled" else "Service paused",
                        fontSize = 13.sp,
                        color = LunaTextSecondary
                    )
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onToggleService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LunaCyan,
                        checkedTrackColor = LunaCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = LunaTextTertiary,
                        uncheckedTrackColor = LunaSurfaceElevated
                    ),
                    modifier = Modifier.testTag("service_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wake word status text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning && hasMicPermission) LunaEmerald else LunaRuby)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = wakeWordStatus,
                    fontSize = 12.sp,
                    color = LunaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Permission status pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionChip(label = "Microphone", granted = hasMicPermission, onClick = onRequestPermissions)
                PermissionChip(label = "Notifications", granted = hasNotificationPermission, onClick = onRequestPermissions)
                PermissionChip(label = "Contacts", granted = hasContactsPermission, onClick = onRequestPermissions)
            }
        }
    }
}

@Composable
private fun PermissionChip(
    label: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (granted) LunaTextPrimary else LunaRuby
            )
        },
        icon = {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) LunaEmerald else LunaRuby,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (granted) LunaEmerald.copy(alpha = 0.12f) else LunaRuby.copy(alpha = 0.12f)
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = if (granted) LunaEmerald.copy(alpha = 0.3f) else LunaRuby.copy(alpha = 0.3f)
        )
    )
}
