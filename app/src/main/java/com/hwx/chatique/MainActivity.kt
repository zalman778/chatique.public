package com.hwx.chatique

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import com.hwx.chatique.flow.IAuthInteractor
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.flow.IStartUpController
import com.hwx.chatique.flow.video.IVideoController
import com.hwx.chatique.flow.voice.IVoiceController
import com.hwx.chatique.helpers.ActivityHolder
import com.hwx.chatique.helpers.INavigationHolder
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.p2p.IE2eController
import com.hwx.chatique.push.NotificationReceiver
import com.hwx.chatique.push.PushService
import com.hwx.chatique.theme.ComposeSampleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

//https://proandroiddev.com/mvi-architecture-with-kotlin-flows-and-channels-d36820b2028d

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authInteractor: IAuthInteractor

    @Inject
    lateinit var snackbarManager: ISnackbarManager

    @Inject
    lateinit var startUpController: IStartUpController

    @Inject
    lateinit var activityHolder: ActivityHolder

    @Inject
    lateinit var e2eController: IE2eController

    @Inject
    lateinit var profileHolder: IProfileHolder

    @Inject
    lateinit var voiceController: IVoiceController

    @Inject
    lateinit var navigationHolder: INavigationHolder

    @Inject
    lateinit var videoController: IVideoController

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voiceController.onRequestPermissionsResult(requestCode, permissions, grantResults)
        videoController.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityHolder.activity = this
        startUpController.onAppStarted()
        handleInputCallNotificationIfExisting()

        setContent {
            ComposeSampleTheme {
                Surface {
                    ChatiqueApp(
                        authInteractor,
                        snackbarManager,
                        profileHolder,
                        e2eController,
                        navigationHolder,
                        videoController,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startUpController.onDestroy()
    }

    private fun handleInputCallNotificationIfExisting() {
        val intent = intent ?: return
        when (intent.action) {
            NotificationReceiver.ACCEPT_CALL -> {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(PushService.NOTIFICATION_ID)
            }
        }
    }
}