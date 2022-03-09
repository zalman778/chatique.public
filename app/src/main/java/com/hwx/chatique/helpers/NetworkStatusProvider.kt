package com.hwx.chatique.helpers

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

interface INetworkStatusProvider {
    fun addListener(listener: INetworkStatusListener)
    fun removeListener(listener: INetworkStatusListener)
    fun isNetworkAvailable(): Boolean
}

interface INetworkStatusListener {
    fun onAvailable() = Unit
    fun onLost() = Unit
    fun onConnectionTypeChanged() = Unit
}

class NetworkStatusProvider(
    context: Context
) : INetworkStatusProvider {

    private val listeners = mutableListOf<INetworkStatusListener>()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val provider: IConnectivityProvider? by lazy {
        if (connectivityManager != null) {
            ConnectivityProvider(connectivityManager, listeners)
        } else null
    }

    override fun addListener(listener: INetworkStatusListener) {
        listeners.add(listener)
        provider?.register()
    }

    override fun removeListener(listener: INetworkStatusListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) provider?.unregister()
    }

    override fun isNetworkAvailable() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        isNetworkAvailableOnMApi()
    } else {
        connectivityManager?.activeNetworkInfo?.isConnected == true
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailableOnMApi() =
        connectivityManager?.run {
            getNetworkCapabilities(activeNetwork)?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } ?: false

    interface IConnectivityProvider {
        fun register()
        fun unregister()
    }
}

class ConnectivityProvider(
    private val connectivityManager: ConnectivityManager,
    private val listeners: List<INetworkStatusListener>
) : NetworkStatusProvider.IConnectivityProvider {

    private val isRegistered = AtomicBoolean(false)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        private var isConnected = false

        override fun onAvailable(network: Network) {
            // log { if (isConnected) "onConnectionTypeChanged" else "onAvailable" }
            listeners.forEach {
                if (isConnected) it.onConnectionTypeChanged() else it.onAvailable()
            }
            isConnected = true
        }

        override fun onLost(network: Network) {
            //log { "onLost" }
            listeners.forEach { it.onLost() }
            isConnected = false
        }

    }

    override fun register() {
        if (isRegistered.get()) return
        isRegistered.set(true)

        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            networkCallback
        )
    }

    override fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        isRegistered.set(false)
    }
}