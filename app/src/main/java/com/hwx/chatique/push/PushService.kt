package com.hwx.chatique.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hwx.chatique.MainActivity
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.R
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@AndroidEntryPoint
class PushService : FirebaseMessagingService() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "Chatique_channel"
        const val CHANNEL_NAME = "Chatique_channel"

        //todo - move to something else
        var pendingCall: PendingCall? = null
    }

    data class PendingCall(
        val userFromId: Long,
        val chatId: String,
    )

    @Inject
    lateinit var repo: IAppRepo

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        GlobalScope.launch {
            repo.setPushToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.containsKey(PushPayload.InputVoiceCall.key)) {
            onInputVoiceMessageReceived(
                message.data[PushPayload.InputVoiceCall.key],
                message.data[PushPayload.PayloadKeyInputCallCaption.key],
            )
        }
        Log.w("AVX", "onMessageReceived: payload = ${message.data} and msg = $message")
        super.onMessageReceived(message)
    }

    private fun onInputVoiceMessageReceived(input: String?, chatCaption: String?) {
        Log.w("AVX", "onInputVoiceMessageReceived: input = $input")
        val params = input.orEmpty().split(";")
        val chatId = params.getOrNull(0) ?: return
        val userFromId = params.getOrNull(1)?.toLongOrNull() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            var mChannel = notificationManager.getNotificationChannel(CHANNEL_ID)

            val soundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.soundtrack)

            if (mChannel == null) {
                mChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                mChannel.setSound(soundUri, attributes)
                notificationManager.createNotificationChannel(mChannel)
            }

            val contentText = chatCaption?.let {
                "$it is Calling ..."
            } ?: "New voice call"

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            builder.setSmallIcon(R.drawable.ic_eye)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setOngoing(true)
                .addAction(R.drawable.ic_call_accept, "Accept", acceptCall(userFromId, chatId))
                .addAction(R.drawable.ic_call_decline, "Decline", declineCall())

            val notification = builder.build()
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            //launching activity
            val intent = Intent(this, MainActivity::class.java)
            intent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(NavigationKeys.Arg.USER_FROM_ID, userFromId)
                putExtra(NavigationKeys.Arg.CHAT_ID, chatId)
            }
            Log.w("AVX", "startActivity: intent = $intent")
            startActivity(intent)
        }
    }

    private fun declineCall(): PendingIntent {
        val intent = Intent()
        intent.action = NotificationReceiver.DECLINE_CALL
        return PendingIntent.getBroadcast(this, 12345, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun acceptCall(userFromId: Long, chatId: String): PendingIntent {
        pendingCall = PendingCall(userFromId, chatId)
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = NotificationReceiver.ACCEPT_CALL
        }
        return PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

enum class PushPayload(val key: String) {
    InputVoiceCall("PayloadKeyInputCall"),
    PayloadKeyInputCallCaption("PayloadKeyInputCallCaption"),
}