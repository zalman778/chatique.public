package com.hwx.chatique.flow

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.hwx.chatique.push.NotificationReceiver

interface ICommunicationServiceInteractor {
    fun startVoicePlayback(userFromId: Long)
    fun startVoiceStreaming(chatId: String)
    fun stopVoicePlayback(userFromId: Long)
    fun stop()
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
class CommunicationServiceInteractor(
    private val context: Context,
) : ICommunicationServiceInteractor {

    override fun startVoicePlayback(userFromId: Long) {
        Log.w("AVX", "CommunicationServiceInteractor startVoicePlayback $userFromId")
        val serviceIntent = Intent(context, CommunicationService::class.java).apply {
            action = CommunicationService.INTENT_START_VOICE_PLAYBACK
            putExtra(CommunicationService.USER_FROM_ID, userFromId)
        }
        context.startService(serviceIntent)
    }

    override fun startVoiceStreaming(chatId: String) {
        Log.w("AVX", "CommunicationServiceInteractor startVoiceStreaming $chatId")
        val serviceIntent = Intent(context, CommunicationService::class.java).apply {
            action = CommunicationService.INTENT_START_VOICE_STREAMING
            putExtra(CommunicationService.CHAT_ID, chatId)
        }
        context.startService(serviceIntent)
    }

    override fun stop() {
        Log.w("AVX", "CommunicationServiceInteractor stop")
        val serviceIntent = Intent(context, CommunicationService::class.java)
            .apply {
                action = NotificationReceiver.DECLINE_CALL
            }
        context.startService(serviceIntent)
        context.stopService(serviceIntent)
    }

    override fun stopVoicePlayback(userFromId: Long) {
        Log.w("AVX", "CommunicationServiceInteractor stopVoicePlayback $userFromId")
        val serviceIntent = Intent(context, CommunicationService::class.java).apply {
            action = CommunicationService.INTENT_STOP_VOICE_PLAYBACK
            putExtra(CommunicationService.USER_FROM_ID, userFromId)
        }
        context.startService(serviceIntent)
    }
}
