package com.prompthavenai.falcibuket.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompthavenai.falcibuket.data.model.ChatMessage
import com.prompthavenai.falcibuket.data.remote.BuketBackend
import com.prompthavenai.falcibuket.data.remote.FortuneError
import com.prompthavenai.falcibuket.data.remote.FortuneErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val loading: Boolean = false,
    val error: FortuneError? = null
)

class ChatViewModel(private val backend: BuketBackend = defaultBackend()) : ViewModel() {

    private var conversationId: String? = null

    private val _state = MutableStateFlow(ChatUiState(loading = true))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val (id, messages) = backend.loadLatestConversation()
                conversationId = id
                _state.value = ChatUiState(
                    messages = if (messages.isEmpty())
                        listOf(ChatMessage(GREETING, fromUser = false))
                    else messages
                )
            } catch (t: Throwable) {
                // Geçmiş yüklenemese de sohbete başlanabilir.
                _state.value = ChatUiState(
                    messages = listOf(ChatMessage(GREETING, fromUser = false)),
                    error = FortuneErrorMapper.fromThrowable(t)
                )
            }
        }
    }

    fun send(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _state.value.sending) return
        val messages = _state.value.messages + ChatMessage(text, fromUser = true, sending = true)
        _state.value = _state.value.copy(messages = messages, sending = true, error = null)
        viewModelScope.launch {
            try {
                val reply = backend.chat(text, conversationId)
                conversationId = reply.conversationId.ifBlank { conversationId }
                _state.value = _state.value.copy(
                    messages = _state.value.messages
                        .map { it.copy(sending = false) }
                        .plus(ChatMessage(reply.reply, fromUser = false)),
                    sending = false
                )
            } catch (t: Throwable) {
                val err = FortuneErrorMapper.fromThrowable(t)
                _state.value = _state.value.copy(
                    messages = _state.value.messages.map {
                        if (it.sending) it.copy(sending = false, failed = true) else it
                    },
                    sending = false,
                    error = err
                )
            }
        }
    }

    fun retryLastFailed() {
        val lastFailed = _state.value.messages.lastOrNull { it.failed && it.fromUser } ?: return
        val cleaned = _state.value.messages.filterNot { it.failed }
        _state.value = _state.value.copy(messages = cleaned)
        send(lastFailed.text)
    }

    companion object {
        const val GREETING =
            "Hoş geldin. Bugün özellikle aşk, kariyer, para veya hayatındaki başka bir konu hakkında mı konuşmak istiyorsun?"

        private val shared = com.prompthavenai.falcibuket.data.remote.FirebaseBuketBackend()
        fun defaultBackend(): BuketBackend = shared
    }
}
