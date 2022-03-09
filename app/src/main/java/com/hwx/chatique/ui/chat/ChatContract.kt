package com.hwx.chatique.ui.chat

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.ChatResponseItem
import com.hwx.chatique.network.models.ChatResponseItemType
import com.hwx.chatique.network.models.MessageEvent
import com.hwx.chatique.ui.communicationRoom.CommunicationRoomViewModel

interface ChatContract {

    data class State(
        val currentUserId: Long = -1L,
        val dialogId: String = "",
        val items: List<MessageEvent> = emptyList(),
        val currentPage: Long = 0L,
        val totalPages: Long = -1L,
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
        val type: ChatResponseItemType = ChatResponseItemType.OPEN,
        val chatInfo: ChatResponseItem = ChatResponseItem.empty(),
    ) : ViewState

    interface Event : ViewEvent {
        data class OnMessageSend(val value: String) : Event
        object OnRetryClick : Event
        object RequestPage : Event
    }

    interface Effect : ViewSideEffect {

        object ScrollToBottom : Effect

        interface Navigation : Effect {
            data class GoToCommunicationRoom(
                val chatId: String,
                val initial: CommunicationRoomViewModel.ScreenInitial,
            ) : Navigation
        }
    }
}