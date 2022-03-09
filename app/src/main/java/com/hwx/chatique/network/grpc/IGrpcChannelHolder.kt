package com.hwx.chatique.network.grpc

import android.content.Context
import com.hwx.chatique.BuildConfig
import io.grpc.Channel
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import io.grpc.okhttp.OkHttpChannelBuilder

interface IGrpcChannelHolder {
    val channel: Channel
    fun start()
    fun stop()
    fun terminate()
}

class GrpcChannelHolder(
    private val context: Context,
    private val interceptors: @JvmSuppressWildcards List<ClientInterceptor>,
) : IGrpcChannelHolder {

    private companion object {
        const val HOST = BuildConfig.ServerAddr
        val PORT = BuildConfig.ServerPort
    }

    override val channel by lazy { buildChannel() }

    override fun start() {
        channel.resetConnectBackoff()
    }

    override fun stop() {
        channel.enterIdle()
    }

    override fun terminate() {
        channel.shutdownNow()
    }

    private fun buildChannel(): ManagedChannel {
        val okHttpChannelBuilder = OkHttpChannelBuilder
            .forAddress(HOST, PORT)

        val androidChannel = AndroidChannelBuilder
            .usingBuilder(okHttpChannelBuilder)
            .context(context)
            .intercept(interceptors)
            .apply {
                //todo - use certificate
                usePlaintext()
            }
            .build()

        return androidChannel
    }
}
