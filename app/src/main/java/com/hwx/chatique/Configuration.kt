package com.hwx.chatique

import android.os.Build
import java.util.*

object Configuration {
    //Rsocket config:
    val RSOCKET_P2P_PORT = getDevicePort()

    private fun getDevicePort(): Int {
        val manufacturer = Build.MANUFACTURER.toUpperCase()
        return if (manufacturer.contains("HUAWEI")) {
            5555
        } else {
            6666
        }
    }

    const val EXTRA_BUFFER_CAPACITY = 5
    const val RSOCKET_PORT = 7878
    const val RSOCKET_KEEP_ALIVE_DURATION_SECS = 40L
    const val RSOCKET_KEEP_ALIVE_MAX_LIFETYME_SECS = 90L
    const val RSOCKET_ACK_PERIOD = 60L
    const val RSOCKET_MISSED_ACKS = 10

    //dh settings:
    const val DH_KEY_SIZE = 256
    const val AES_INIT_VECTOR = "INIT_VECTOR12345"

    val TEMP_PROFILE_UUID = UUID.randomUUID().toString()
}