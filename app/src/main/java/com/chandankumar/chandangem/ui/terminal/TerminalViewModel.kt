package com.chandankumar.chandangem.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandankumar.chandangem.core.terminal.TerminalManager
import com.chandankumar.chandangem.core.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TerminalViewModel(
    private val terminalManager: TerminalManager
) : ViewModel() {

    val sessions: StateFlow<List<TerminalSession>> = terminalManager.sessionsList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    private val _commandInput = MutableStateFlow("")
    val commandInput: StateFlow<String> = _commandInput.asStateFlow()

    init {
        // Auto-create initial session if none exists
        viewModelScope.launch {
            if (sessions.value.isEmpty()) {
                val initial = terminalManager.createSession("Termux-1")
                _selectedSessionId.value = initial.id
            } else {
                _selectedSessionId.value = sessions.value.first().id
            }
        }
    }

    fun selectSession(sessionId: String) {
        _selectedSessionId.value = sessionId
    }

    fun updateCommandInput(text: String) {
        _commandInput.value = text
    }

    fun createNewSession() {
        val session = terminalManager.createSession()
        _selectedSessionId.value = session.id
    }

    fun closeSelectedSession() {
        val currentId = _selectedSessionId.value ?: return
        terminalManager.closeSession(currentId)
        _selectedSessionId.value = sessions.value.firstOrNull()?.id
    }

    fun sendCommand() {
        val cmd = _commandInput.value
        if (cmd.isBlank()) return

        val currentId = _selectedSessionId.value ?: return
        val session = terminalManager.getSession(currentId) ?: return

        session.sendInput(cmd)
        _commandInput.value = ""
    }

    fun clearCurrentScrollback() {
        val currentId = _selectedSessionId.value ?: return
        terminalManager.getSession(currentId)?.clearScrollback()
    }
}
