package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandHistoryEntity
import com.example.ui.theme.LunaBorderGlow
import com.example.ui.theme.LunaCyan
import com.example.ui.theme.LunaEmerald
import com.example.ui.theme.LunaRuby
import com.example.ui.theme.LunaSurfaceDark
import com.example.ui.theme.LunaSurfaceElevated
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.LunaTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LunaHistoryDialog(
    historyList: List<CommandHistoryEntity>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("luna_history_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = LunaSurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent Commands",
                    color = LunaTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = LunaTextTertiary
                        )
                    }
                }
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No command history yet.",
                        color = LunaTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Commands executed by LUNA will show here.",
                        color = LunaTextTertiary,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList) { item ->
                        CommandHistoryItem(item)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = LunaCyan)
            }
        }
    )
}

@Composable
private fun CommandHistoryItem(item: CommandHistoryEntity) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LunaSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LunaBorderGlow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.executionSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (item.executionSuccess) LunaEmerald else LunaRuby,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.identifiedIntent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LunaCyan
                    )
                }
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = LunaTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${item.spokenQuery}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = LunaTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.responseText,
                fontSize = 12.sp,
                color = LunaTextSecondary
            )
        }
    }
}
