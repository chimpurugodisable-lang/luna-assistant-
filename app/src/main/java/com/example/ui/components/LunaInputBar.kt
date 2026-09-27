package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LunaBorderGlow
import com.example.ui.theme.LunaCyan
import com.example.ui.theme.LunaSurfaceDark
import com.example.ui.theme.LunaSurfaceElevated
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.LunaTextTertiary

@Composable
fun LunaInputBar(
    onSendTextCommand: (text: String) -> Unit,
    onVoiceTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = {
                Text(
                    text = "Type a command or question...",
                    fontSize = 13.sp,
                    color = LunaTextTertiary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LunaSurfaceElevated,
                unfocusedContainerColor = LunaSurfaceDark,
                focusedBorderColor = LunaCyan,
                unfocusedBorderColor = LunaBorderGlow,
                focusedTextColor = LunaTextPrimary,
                unfocusedTextColor = LunaTextPrimary,
                cursorColor = LunaCyan
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (textInput.isNotBlank()) {
                        onSendTextCommand(textInput.trim())
                        textInput = ""
                        focusManager.clearFocus()
                    }
                }
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("luna_text_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (textInput.isNotBlank()) {
            FilledIconButton(
                onClick = {
                    onSendTextCommand(textInput.trim())
                    textInput = ""
                    focusManager.clearFocus()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = LunaCyan,
                    contentColor = Color.Black
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command"
                )
            }
        } else {
            FilledIconButton(
                onClick = onVoiceTrigger,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = LunaSurfaceElevated,
                    contentColor = LunaCyan
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("manual_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Manual Voice Input"
                )
            }
        }
    }
}
