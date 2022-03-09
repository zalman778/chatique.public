package com.hwx.chatique.flow

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.arch.extensions.equalsToOneOf
import com.hwx.chatique.helpers.INavigationHolder
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.models.MessageMetaEvent
import com.hwx.chatique.network.models.MessageMetaType
import com.hwx.chatique.push.PushService
import com.hwx.chatique.ui.communicationRoom.CommunicationRoomViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlin.coroutines.CoroutineContext

/*
    Entity, which listens streams for input calls.
 */
interface ICommunicationListener {
    fun onAuthenticated()
    fun onDestroy()
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
class CommunicationListener(
    private val streamManager: IStreamManager,
    private val navigationHolder: INavigationHolder,
) : ICommunicationListener, CoroutineScope {

    private val innerJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = innerJob + Dispatchers.IO


    override fun onAuthenticated() {
        launch(Dispatchers.Main) {
            streamManager.getStreamInput<MessageMetaEvent>(IStreamManager.StreamId.META_EVENTS)
                .filter {
                    it.type.equalsToOneOf(
                        MessageMetaType.REQUEST_VOICE_CALL,
                    )
                }
                .collect(::onNewMetaEvent)
        }

        checkForPendingIncomingCall()
    }

    override fun onDestroy() {
        innerJob.cancelChildren()
    }

    private fun onNewMetaEvent(it: MessageMetaEvent) {
        when (it.type) {
            MessageMetaType.REQUEST_VOICE_CALL -> onVoiceCallRequest(it)
            else -> Unit
        }
    }

    private fun onVoiceCallRequest(it: MessageMetaEvent) {
        val navController = navigationHolder.controller ?: return
        val navPath =
            "${NavigationKeys.Route.COMMUNICATION_ROOM}?${NavigationKeys.Arg.CHAT_ID}=${it.chatId}&${NavigationKeys.Arg.USER_FROM_ID}=${it.userFromId}&${NavigationKeys.Arg.COMM_ROOM_SCREEN_INITIAL}=${CommunicationRoomViewModel.ScreenInitial.INCOMING.name}"
        navController.navigate(navPath)
    }

    private fun checkForPendingIncomingCall() {
        val pendingCall = PushService.pendingCall ?: return
        val navController = navigationHolder.controller ?: return
        val navPath =
            "${NavigationKeys.Route.COMMUNICATION_ROOM}?${NavigationKeys.Arg.CHAT_ID}=${pendingCall.chatId}&${NavigationKeys.Arg.USER_FROM_ID}=${pendingCall.userFromId}&${NavigationKeys.Arg.COMM_ROOM_SCREEN_INITIAL}=${CommunicationRoomViewModel.ScreenInitial.INCOMING_WITH_AUTO_ACCEPT.name}"
        GlobalScope.launch(Dispatchers.Main) {
            delay(300)
            navController.navigate(navPath)
        }
    }
}