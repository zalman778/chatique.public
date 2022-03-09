package com.hwx.chatique.ui.friendshipRequests

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.FriendshipRequestType
import com.hwx.chatique.network.models.FriendshipRequestsItem

interface FriendshipRequestsContract {

    data class State(
        val type: FriendshipRequestType = FriendshipRequestType.INPUT,
        val items: List<FriendshipRequestsItem> = emptyList(),
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        object OnSwitchTypeClick : Event
        data class OnItemAccept(val id: String) : Event
        data class OnItemDecline(val id: String) : Event
        object OnRetryClick : Event
    }

    interface Effect : ViewSideEffect {
        interface Navigation : Effect
    }
}