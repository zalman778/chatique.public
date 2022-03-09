package com.hwx.chatique.network.grpc

import android.util.Log
import com.hwx.chatique.arch.network.ClientCallListener
import com.hwx.chatique.flow.IAuthInteractor
import io.grpc.*

class GrpcDeAuthInterceptor : ClientInterceptor {

    companion object {
        var authInteractor: IAuthInteractor? = null
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel
    ) = object : ForwardingClientCall<ReqT, RespT>() {

        private val delegate = next.newCall(method, callOptions)

        override fun delegate() = delegate

        override fun start(responseListener: Listener<RespT>, headers: Metadata?) {
            delegate.start(object : ClientCallListener<RespT>(responseListener) {

                override fun onClose(status: Status?, trailers: Metadata?) {
                    Log.w("AVX", "onClose: status = $status")
                    if (status?.code == Status.Code.UNAUTHENTICATED) {
                        authInteractor?.logout()
                    }

                    responseListener.onClose(status, trailers)
                }
            }, headers)
        }
    }
}