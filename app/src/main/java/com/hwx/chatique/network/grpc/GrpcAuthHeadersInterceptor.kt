package com.hwx.chatique.network.grpc

import com.hwx.chatique.arch.network.GrpcHeadersInterceptor
import io.grpc.ClientInterceptor
import io.grpc.Metadata

class GrpcAuthHeadersInterceptor {

    companion object {
        var token = ""
        private val KEY_AUTH: Metadata.Key<String>? =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    }

    fun authHeaders(): ClientInterceptor =
        GrpcHeadersInterceptor {
            Metadata().apply {
                if (token.isNotEmpty()) {
                    put(KEY_AUTH, "Bearer $token")
                }
            }
        }
}

