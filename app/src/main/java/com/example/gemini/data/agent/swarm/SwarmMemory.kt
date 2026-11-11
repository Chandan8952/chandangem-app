package com.example.gemini.data.agent.swarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class SwarmMemory {
    private val contextStore = ConcurrentHashMap<String, String>()
    private val artifactStore = ConcurrentHashMap<String, String>()

    private val _messages = MutableStateFlow<List<SwarmMessage>>(emptyList())
    val messages: StateFlow<List<SwarmMessage>> = _messages.asStateFlow()

    fun putContext(key: String, value: String) {
        contextStore[key] = value
    }

    fun getContext(key: String): String? = contextStore[key]

    fun getAllContext(): Map<String, String> = contextStore.toMap()

    fun putArtifact(name: String, content: String) {
        artifactStore[name] = content
    }

    fun getArtifact(name: String): String? = artifactStore[name]

    fun getAllArtifacts(): Map<String, String> = artifactStore.toMap()

    fun postMessage(message: SwarmMessage) {
        val current = _messages.value
        _messages.value = current + message
    }

    fun getMessagesForRole(role: SwarmRole): List<SwarmMessage> {
        return _messages.value.filter { it.toRole == null || it.toRole == role }
    }

    fun clear() {
        contextStore.clear()
        artifactStore.clear()
        _messages.value = emptyList()
    }
}
