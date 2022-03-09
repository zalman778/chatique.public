package com.hwx.chatique.ui.communicationRoom

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.R
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.extensions.equalsToOneOf
import com.hwx.chatique.flow.ICommunicationServiceInteractor
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.flow.video.IVideoController
import com.hwx.chatique.flow.voice.IVoiceController
import com.hwx.chatique.helpers.ActivityHolder
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.models.MessageMetaEvent
import com.hwx.chatique.network.models.MessageMetaType
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


@HiltViewModel
class CommunicationRoomViewModel @Inject constructor(
    private val stateHandle: SavedStateHandle,
    private val profileHolder: IProfileHolder,
    private val streamManager: IStreamManager,
    private val serviceInteractor: ICommunicationServiceInteractor,
    private val activityHolder: ActivityHolder,
    private val repo: IAppRepo,
    private val voiceController: IVoiceController,
    private val videoController: IVideoController,
) : BaseViewModel<CommunicationRoomContract.Event, CommunicationRoomContract.State, CommunicationRoomContract.Effect>() {

    enum class ScreenInitial {
        INCOMING,
        INCOMING_WITH_AUTO_ACCEPT,
        OUTGOING,
        CONNECT_TO_EXISTING;

        companion object {
            fun fromString(value: String) = values().find { it.name == value }
        }
    }

    enum class VoiceCallResponse {
        ACCEPT,
        DECLINE;

        companion object {
            fun fromString(value: String) = values().find { it.name == value }
        }
    }

    private var chatId = ""
    private var userFromId: Long? = null
    private var screenInitial: ScreenInitial? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isAcceptCallClicked = false

    //для кейса когда чел не сразу принял участие в конфе, и чтобы он не пропустил ассепты других, пока не участвует
    private val pendingAcceptsFromMembers = mutableSetOf<Long>()

    fun init(isRefreshed: Boolean): Boolean {
        if (!isRefreshed) {
            loadParams()
            requestChatInfo()
            initInternal()
        }
        return true
    }

    private fun requestChatInfo() {
        viewModelScope.launch {
            when (val result = repo.getChatInfo(chatId)) {
                is Result.Success -> {
                    setState {
                        copy(chatInfo = result.value)
                    }
                    onChatInfoLoaded()
                }
            }
        }
    }

    override fun setInitialState() = CommunicationRoomContract.State()

    override fun handleEvents(event: CommunicationRoomContract.Event) = when (event) {
        CommunicationRoomContract.Event.OnAcceptCallClick -> onAcceptCallClick()
        CommunicationRoomContract.Event.OnDeclineCallClick -> onDeclineCallClick()
        CommunicationRoomContract.Event.OnMuteClick -> onMuteClick()
        CommunicationRoomContract.Event.OnVideoClick -> onVideoClick()
        CommunicationRoomContract.Event.OnCameraSwitchClick -> videoController.switchCamera()
        CommunicationRoomContract.Event.OnSpeakerClick -> onSpeakerClick()
        else -> Unit
    }

    private fun loadParams() {
        chatId = stateHandle.get<String>(NavigationKeys.Arg.CHAT_ID).orEmpty()
        userFromId = stateHandle.get<Long>(NavigationKeys.Arg.USER_FROM_ID)
        screenInitial = stateHandle.get<String>(NavigationKeys.Arg.COMM_ROOM_SCREEN_INITIAL)?.let {
            ScreenInitial.fromString(it)
        }
    }

    private fun initInternal() {
        viewModelScope.launch {
            streamManager.getStreamInput<MessageMetaEvent>(IStreamManager.StreamId.META_EVENTS)
                .filter {
                    it.type == MessageMetaType.VOICE_CALL_RESPONSE && it.chatId == chatId
                }
                .collect(::onVoiceCallResponse)
        }

        viewModelScope.launch {
            streamManager.getStreamInput<MessageMetaEvent>(IStreamManager.StreamId.META_EVENTS)
                .filter {
                    it.type == MessageMetaType.VIDEO_CALL_PAYLOAD && it.chatId == chatId
                }
                .collect(::onVideoCallPayload)
        }

        when (screenInitial) {
            ScreenInitial.INCOMING -> {
                startRingtone()
                setState {
                    copy(state = CommunicationRoomContract.CallState.INCOMING)
                }
            }
            ScreenInitial.INCOMING_WITH_AUTO_ACCEPT -> onAcceptCallClick()
            ScreenInitial.OUTGOING -> {
                setState {
                    copy(state = CommunicationRoomContract.CallState.OUTGOING)
                }
                sendOutgoingCallRequest()
            }
        }
    }

    private fun onChatInfoLoaded() {
        when (screenInitial) {
            ScreenInitial.CONNECT_TO_EXISTING -> {
                onAcceptCallClick()
                //we should start playback for all communicating members
                val commMembersList = viewState.value.chatInfo.members
                    .filter { it.isCommunicating && it.userId != profileHolder.userId }
                    .map { it.userId }
                Log.w("AVX", "CONNECT_TO_EXISTING: commList = $commMembersList")
                commMembersList.forEach {
                    serviceInteractor.startVoicePlayback(it)
                }
            }
        }
    }

    private fun updateStateByVoiceCallResponse(
        userFromId: Long,
        response: VoiceCallResponse,
    ) {
        val isCommunicating = response == VoiceCallResponse.ACCEPT
        val newList = viewState.value.chatInfo.members.toMutableList().apply {
            val targetMember = this.find { it.userId == userFromId } ?: return@apply
            remove(targetMember)
            add(targetMember.copy(isCommunicating = isCommunicating))
        }
        setState {
            copy(
                chatInfo = chatInfo.copy(
                    members = newList,
                )
            )
        }
    }

    private fun startRingtone() {
        val context = activityHolder.activity ?: return
        mediaPlayer = MediaPlayer.create(context, R.raw.soundtrack)
        mediaPlayer?.start()
    }

    private fun stopRingtone() {
        mediaPlayer?.stop()
    }

    private fun onAcceptCallClick() {
        isAcceptCallClicked = true
        updateStateByVoiceCallResponse(profileHolder.userId, VoiceCallResponse.ACCEPT)
        stopRingtone()
        sendAccept()
        serviceInteractor.startVoiceStreaming(chatId)
        userFromId?.takeIf { it != -1L }?.let {
            serviceInteractor.startVoicePlayback(it)
        }
        if (pendingAcceptsFromMembers.isNotEmpty()) {
            pendingAcceptsFromMembers.forEach {
                serviceInteractor.startVoicePlayback(it)
            }
        }
        setState {
            copy(state = CommunicationRoomContract.CallState.IN_PROCESS)
        }
    }

    private fun onDeclineCallClick() {
        isAcceptCallClicked = false
        stopRingtone()
        videoController.stop()
        updateStateByVoiceCallResponse(profileHolder.userId, VoiceCallResponse.DECLINE)
        when (viewState.value.state) {
            CommunicationRoomContract.CallState.IN_PROCESS -> {
                serviceInteractor.stop()
                sendDecline()
            }
            CommunicationRoomContract.CallState.INCOMING -> {
                sendDecline()
            }
        }
        setState {
            copy(
                state = CommunicationRoomContract.CallState.CANCELLED,
                isShowingOpponentCameraPreview = false,
                isShowingSelfCameraPreview = false,
            )
        }
    }

    private fun onMuteClick() {
        voiceController.toggleMute()
        setState {
            copy(isMuted = !isMuted)
        }
    }

    private fun onSpeakerClick() {
        voiceController.toggleSpeaker()
        setState {
            copy(isUsingSpeaker = !isUsingSpeaker)
        }
    }

    private fun onVideoClick() {
        videoController.startStreaming(chatId)
        videoController.startPlayback(chatId)
        setState {
            copy(isShowingSelfCameraPreview = true)
        }
    }

    private fun sendAccept() {
        val event = MessageMetaEvent(
            UUID.randomUUID().toString(),
            userFromId.toString(),
            chatId,
            MessageMetaType.VOICE_CALL_RESPONSE,
            VoiceCallResponse.ACCEPT.name,
            Date().time / 1000,
            profileHolder.userId,
        )

        Log.w("AVX", "sending accept = $event")

        streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
    }

    private fun sendDecline() {
        val event = MessageMetaEvent(
            UUID.randomUUID().toString(),
            "",
            chatId,
            MessageMetaType.VOICE_CALL_RESPONSE,
            VoiceCallResponse.DECLINE.name,
            Date().time / 1000,
            profileHolder.userId,
        )

        Log.w("AVX", "sending decline = $event")

        streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
    }

    private fun sendOutgoingCallRequest() {
        val event = MessageMetaEvent(
            UUID.randomUUID().toString(),
            "",
            chatId,
            MessageMetaType.REQUEST_VOICE_CALL,
            "",
            Date().time / 1000,
            profileHolder.userId,
        )

        streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
    }

    private fun onVoiceCallResponse(it: MessageMetaEvent) {
        val response = VoiceCallResponse.fromString(it.value) ?: return
        updateStateByVoiceCallResponse(it.userFromId, response)
        when (response) {
            VoiceCallResponse.DECLINE -> onRemoteDeclined(it)
            VoiceCallResponse.ACCEPT -> onRemoteAccepted(it)
        }
    }

    private fun onVideoCallPayload(it: MessageMetaEvent) {
        if (!viewState.value.isShowingOpponentCameraPreview) {
            setState {
                copy(isShowingOpponentCameraPreview = true)
            }
        }
        videoController.putOpponentPayload(it)
    }

    private fun onRemoteAccepted(it: MessageMetaEvent) {
        Log.w("AVX", "onRemoteAccepted = $it")

        //тут мы должны реагировать только если приняли звонок
        //кейс когда чел, к-рый не принял звонок начинает слушать тех, кто подключился

        if (isAcceptCallClicked || screenInitial.equalsToOneOf(
                ScreenInitial.OUTGOING,
                ScreenInitial.CONNECT_TO_EXISTING
            )
        ) {
            serviceInteractor.startVoicePlayback(it.userFromId)
            serviceInteractor.startVoiceStreaming(chatId)
        }

        if (!isAcceptCallClicked) {
            pendingAcceptsFromMembers.add(it.userFromId)
        }

        //дополнительно отсылаем accept, если мы были инициатором звонка
        //кейс с бексонечным пинг-понгом call accept
        if (screenInitial == ScreenInitial.OUTGOING) {
            sendAccept()
            updateStateByVoiceCallResponse(profileHolder.userId, VoiceCallResponse.ACCEPT)
        }
    }

    private fun onRemoteDeclined(it: MessageMetaEvent) {
        Log.w("AVX", "onRemoteDeclined = $it")
        serviceInteractor.stopVoicePlayback(it.userFromId)

        if (!isAcceptCallClicked) {
            if (pendingAcceptsFromMembers.contains(it.userFromId)) {
                pendingAcceptsFromMembers.remove(it.userFromId)
            }
        }
    }
}