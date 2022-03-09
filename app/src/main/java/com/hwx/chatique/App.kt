package com.hwx.chatique

import android.app.Application
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var appRepo: IAppRepo

    override fun onCreate() {
        super.onCreate()
        generateFirebaseToken()
    }

    private fun generateFirebaseToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("AVX", "Fetching FCM registration token failed", task.exception)
                    //todo - use normal scope
                    GlobalScope.launch {
                        appRepo.setPushToken("")
                    }
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token: String? = task.result

                //todo - use normal scope
                GlobalScope.launch {
                    appRepo.setPushToken(token!!)
                }

                // Log and toast
                Log.d("AVX", "token = $token")
            })
    }
}