package com.hwx.chatique.push

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACCEPT_CALL = "ACCEPT_CALL"
        const val DECLINE_CALL = "DECLINE_CALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DECLINE_CALL -> {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(PushService.NOTIFICATION_ID)

                //todo - should we send decline answer or no?
            }
        }
    }
}