package com.hwx.chatique.arch.network

import io.grpc.*

class GrpcHeadersInterceptor(
    private val metadataProvider: () -> Metadata
) : ClientInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel,
    ): ClientCall<ReqT, RespT> =
        object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {

            override fun start(responseListener: Listener<RespT>?, headers: Metadata?) {
                headers?.merge(metadataProvider())
                super.start(responseListener, headers)
            }
        }
}
