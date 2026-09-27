package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.LunaVoiceState
import com.example.data.LunaDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.service.LunaAssistantManager
import com.example.ui.components.LunaDiagnosticsCard
import com.example.ui.components.LunaHistoryDialog
import com.example.ui.components.LunaInputBar
import com.example.ui.components.LunaOrbVisualizer
import com.example.ui.components.LunaQuickChips
import com.example.ui.components.LunaTranscriptCard
import com.example.ui.theme.LunaCyan
import com.example.ui.theme.LunaSpaceDark
import com.example.ui.theme.LunaSurfaceDark
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.LunaViolet
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunaMainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val status by LunaAssistantManager.status.collectAsState()
    val rms by LunaAssistantManager.rmsAmplitude.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyList by remember { mutableStateOf<List<CommandHistoryEntity>>(emptyList()) }

    // Check permissions
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    var hasMicPermission by remember { mutableStateOf(checkPermission(Manifest.permission.RECORD_AUDIO)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(Manifest.permission.POST_NOTIFICATIONS)
            } else true
        )
    }
    var hasContactsPermission by remember { mutableStateOf(checkPermission(Manifest.permission.READ_CONTACTS)) }
    var hasSmsPermission by remember { mutableStateOf(checkPermission(Manifest.permission.SEND_SMS)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] ?: hasMicPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
        hasContactsPermission = results[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission
        hasSmsPermission = results[Manifest.permission.SEND_SMS] ?: hasSmsPermission

        LunaAssistantManager.setMicPermissionGranted(hasMicPermission)

        if (hasMicPermission) {
            LunaAssistantManager.startService(context)
        }
    }

    LaunchedEffect(Unit) {
        hasMicPermission = checkPermission(Manifest.permission.RECORD_AUDIO)
        LunaAssistantManager.setMicPermissionGranted(hasMicPermission)

        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        permissionsToRequest.add(Manifest.permission.SEND_SMS)

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Refresh history when dialog is shown
    LaunchedEffect(showHistoryDialog) {
        if (showHistoryDialog) {
            scope.launch {
                val db = LunaDatabase.getInstance(context)
                historyList = db.commandHistoryDao().getRecentHistory().firstOrNull() ?: emptyList()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("luna_main_scaffold"),
        containerColor = LunaSpaceDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(LunaCyan, LunaViolet))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = LunaSpaceDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LUNA",
                                color = LunaTextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "AI Voice Assistant",
                                color = LunaTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Command History",
                            tint = LunaCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LunaSpaceDark
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LunaSpaceDark)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LunaQuickChips(
                    onChipClicked = { cmd ->
                        LunaAssistantManager.submitTextCommand(cmd)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LunaInputBar(
                    onSendTextCommand = { text ->
                        LunaAssistantManager.submitTextCommand(text)
                    },
                    onVoiceTrigger = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            LunaAssistantManager.triggerVoiceCommand()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Interactive Lunar Orb Visualizer
            LunaOrbVisualizer(
                state = status.state,
                rmsAmplitude = rms,
                onClick = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    } else {
                        LunaAssistantManager.triggerVoiceCommand()
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tap hint
            Text(
                text = when (status.state) {
                    LunaVoiceState.IDLE -> "Say \"Luna\" or \"Hey Luna\" anytime • Tap orb to talk"
                    LunaVoiceState.LISTENING_FOR_WAKE_WORD -> "Listening for \"Luna\" • Hands-free active"
                    LunaVoiceState.WAKE_WORD_DETECTED -> "Wake phrase recognized"
                    LunaVoiceState.LISTENING_FOR_COMMAND -> "Listening to your voice..."
                    LunaVoiceState.THINKING -> "Processing your command..."
                    LunaVoiceState.EXECUTING -> "Performing action..."
                    LunaVoiceState.SPEAKING -> "Speaking result..."
                    LunaVoiceState.ERROR -> "Check microphone permissions"
                },
                fontSize = 12.sp,
                color = LunaTextSecondary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Real-time Transcript Card
            LunaTranscriptCard(
                state = status.state,
                spokenText = status.lastSpokenUserText,
                assistantResponse = status.lastAssistantResponse
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Background Service & Hardware Status Card
            LunaDiagnosticsCard(
                isServiceRunning = status.isServiceRunning,
                onToggleService = { enable ->
                    if (enable) {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            LunaAssistantManager.startService(context)
                        }
                    } else {
                        LunaAssistantManager.stopService(context)
                    }
                },
                wakeWordStatus = status.wakeWordEngineStatus,
                hasMicPermission = hasMicPermission,
                hasNotificationPermission = hasNotificationPermission,
                hasContactsPermission = hasContactsPermission,
                onRequestPermissions = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.CALL_PHONE
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showHistoryDialog) {
        LunaHistoryDialog(
            historyList = historyList,
            onDismiss = { showHistoryDialog = false },
            onClearHistory = {
                scope.launch {
                    val db = LunaDatabase.getInstance(context)
                    db.commandHistoryDao().clearHistory()
                    historyList = emptyList()
                }
            }
        )
    }
}
