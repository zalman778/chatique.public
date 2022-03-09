package com.hwx.chatique.ui.userSearch

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.SearchUserResponseItem

interface UserSearchContract {

    data class State(
        val items: List<SearchUserResponseItem> = emptyList(),
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        object OnRetryClick : Event
        data class SearchStrChanged(val value: String) : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {
            data class OnUserClick(val id: Long, val username: String) : Navigation
        }
    }
}