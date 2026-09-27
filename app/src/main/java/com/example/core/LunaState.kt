package com.example.core

enum class LunaVoiceState(val label: String) {
    IDLE("Sleeping"),
    LISTENING_FOR_WAKE_WORD("Listening for \"Luna\""),
    WAKE_WORD_DETECTED("Wake Word Detected"),
    LISTENING_FOR_COMMAND("Listening..."),
    THINKING("Thinking..."),
    EXECUTING("Executing..."),
    SPEAKING("Speaking..."),
    ERROR("Needs Attention")
}

data class AssistantStatus(
    val state: LunaVoiceState = LunaVoiceState.IDLE,
    val lastSpokenUserText: String = "",
    val lastAssistantResponse: String = "LUNA is ready. Say \"Luna\" or \"Hey Luna\" to start.",
    val isMicPermissionGranted: Boolean = false,
    val isServiceRunning: Boolean = false,
    val wakeWordEngineStatus: String = "Offline wake-word ready",
    val activeMediaApp: String? = null,
    val errorMessage: String? = null
)
