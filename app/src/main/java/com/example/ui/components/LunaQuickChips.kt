package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LunaBorderGlow
import com.example.ui.theme.LunaSurfaceElevated
import com.example.ui.theme.LunaTextPrimary

@Composable
fun LunaQuickChips(
    onChipClicked: (command: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleCommands = listOf(
        "Open WhatsApp",
        "Open WhatsApp Business",
        "Play music",
        "Call John",
        "Set alarm for 6 AM",
        "Remind me to study at 7 PM",
        "Search scholarships in Canada",
        "Open Wi-Fi settings",
        "Explain quantum computing"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sampleCommands.forEachIndexed { index, cmd ->
            SuggestionChip(
                onClick = { onChipClicked(cmd) },
                label = {
                    Text(
                        text = cmd,
                        fontSize = 12.sp,
                        color = LunaTextPrimary
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = LunaSurfaceElevated.copy(alpha = 0.8f)
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = LunaBorderGlow
                ),
                modifier = Modifier.testTag("quick_chip_$index")
            )
        }
    }
}
