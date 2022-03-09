package com.hwx.chatique.ui.groupChatCreation

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.FriendsListItemResponse

interface GroupChatCreationContract {

    data class State(
        val items: List<FriendsListItemResponse> = emptyList(),
        val isSecret: Boolean = false,
        val selectedList: List<String> = emptyList(),
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        data class OnItemToggle(val id: String) : Event
        object OnRetryClick : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {

            data class GoToChat(
                val isSecret: Boolean,
                val targetUserIds: List<String> = emptyList(),
            ) : Navigation
        }
    }
}