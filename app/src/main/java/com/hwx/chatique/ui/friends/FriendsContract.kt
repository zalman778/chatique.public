package com.hwx.chatique.ui.friends

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.FriendsListItemResponse

interface FriendsContract {

    data class State(
        val items: List<FriendsListItemResponse> = emptyList(),
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        data class OnItemRemove(val id: String) : Event
        object OnRetryClick : Event
        data class OnFriendRemovalConfirmed(val id: Long) : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {
            data class GoToUserDetails(val id: String, val username: String) : Navigation
            object GoToUserSearch : Navigation
        }
    }
}