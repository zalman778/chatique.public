package com.hwx.chatique.ui.communicationRoom

import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState
import com.hwx.chatique.network.models.ChatResponseItem

interface CommunicationRoomContract {

    enum class CallState {
        NONE,
        INCOMING,
        OUTGOING,
        IN_PROCESS,
        CANCELLED,
    }

    data class State(
        //val items: List<FriendsListItemResponse> = emptyList(),
        val callStartedAt: Long = -1L,
        val errorStr: String = "",
        val state: CallState = CallState.NONE,
        val chatInfo: ChatResponseItem = ChatResponseItem.empty(),
        val isMuted: Boolean = false,
        val isUsingSpeaker: Boolean = false,
        val isShowingSelfCameraPreview: Boolean = false,
        val isShowingOpponentCameraPreview: Boolean = false,
    ) : ViewState

    interface Event : ViewEvent {
        object OnAcceptCallClick : Event
        object OnDeclineCallClick : Event
        object OnMuteClick : Event
        object OnVideoClick : Event
        object OnCameraSwitchClick : Event
        object OnSpeakerClick : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {
//            data class GoToUserDetails(val id: String, val username: String) : Navigation
//            object GoToUserSearch : Navigation
        }
    }
}