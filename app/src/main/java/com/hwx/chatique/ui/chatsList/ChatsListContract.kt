package com.hwx.chatique.ui.chatsList

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.ChatResponseItem

interface ChatsListContract {

    data class State(
        val items: List<ChatResponseItem> = emptyList(),
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        object OnRetryClick : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {
            data class GoToChat(
                val id: String,
                val isSecret: Boolean,
                val targetUserId: Long = -1L
            ) : Navigation

            data class GoToChatCreation(
                val isSecret: Boolean = false,
            ) : Navigation
        }
    }
}