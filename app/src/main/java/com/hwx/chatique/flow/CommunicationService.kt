package com.hwx.chatique.flow

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import com.hwx.chatique.MainActivity
import com.hwx.chatique.R
import com.hwx.chatique.flow.voice.IVoiceController
import com.hwx.chatique.push.NotificationReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import javax.inject.Inject

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@AndroidEntryPoint
class CommunicationService : Service(), CoroutineScope {

    companion object {
        const val CHAT_ID = "CHAT_ID"
        const val USER_FROM_ID = "USER_FROM_ID"
        const val INTENT_START_VOICE_PLAYBACK = "INTENT_START_VOICE_PLAYBACK"
        const val INTENT_START_VOICE_STREAMING = "INTENT_START_VOICE_STREAMING"
        const val INTENT_STOP_VOICE_PLAYBACK = "INTENT_STOP_VOICE_PLAYBACK"
    }

    private val serviceJob = Job()
    override val coroutineContext = serviceJob + Dispatchers.IO

    @Inject
    lateinit var voiceController: IVoiceController

    override fun onBind(p0: Intent?): Nothing? = null

    override fun onCreate() {
        Log.d("AVX", "TrackingService onCreate")
        super.onCreate()
        makeForeground()
    }

    override fun onDestroy() {
        serviceJob.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            "AVX",
            "TrackingService onStartCommand with chatId = ${intent?.extras?.get(CHAT_ID)} intent = $intent"
        )

        when (intent?.action) {
            NotificationReceiver.DECLINE_CALL -> {
                voiceController.stop()
                val serviceIntent = Intent(this, CommunicationService::class.java)
                stopService(serviceIntent)
            }
            INTENT_START_VOICE_PLAYBACK -> {
                val userFromId = intent.extras?.get(USER_FROM_ID) as? Long
                userFromId?.let {
                    voiceController.startVoicePlayback(it)
                }
            }
            INTENT_START_VOICE_STREAMING -> {
                val chatId = intent.extras?.get(CHAT_ID) as? String
                chatId?.let {
                    voiceController.startVoiceStreaming(it)
                }
            }
            INTENT_STOP_VOICE_PLAYBACK -> {
                val userFromId = intent.extras?.get(USER_FROM_ID) as? Long
                userFromId?.let {
                    voiceController.stopVoicePlayback(it)
                }
            }
        }

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun makeForeground() {
        val notification = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            createNotificationWithChannel()
        else
            createNotification()
        startForeground(
            1,
            notification,
        )
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this)

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val intent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification: Notification = notificationBuilder
            .setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_eye)
            .setContentText("communication in progress")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            //.setContentIntent(intent)
            .addAction(R.drawable.ic_call_decline, "Decline", declineCall())
            .build()
        //notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        return notification
    }

    private fun declineCall(): PendingIntent {
        val intent = Intent()
        intent.action = NotificationReceiver.DECLINE_CALL
        return PendingIntent.getService(this, 0, intent, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationWithChannel(): Notification {
        val NOTIFICATION_CHANNEL_ID = "com.getlocationbackground"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val intent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_eye)
            .setContentText("communication in progress")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            //.setContentIntent(intent)
            .addAction(R.drawable.ic_call_decline, "Decline", declineCall())
            .build()
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        return notification
    }
}