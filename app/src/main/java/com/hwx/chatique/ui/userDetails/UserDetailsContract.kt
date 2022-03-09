package com.hwx.chatique.ui.userDetails

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState

interface UserDetailsContract {

    data class State(
        val id: Long = 0L,
        val username: String = "",
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        object OnRetryClick : Event
        object OnAddToFriendRequested : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {
            data class OnUserClick(val id: Long) : Navigation
            data class OnOpenChatClick(val id: Long) : Navigation
            data class OnOpenSecretChatClick(val id: Long) : Navigation
        }
    }
}