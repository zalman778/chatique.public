package com.hwx.chatique.arch.network

import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.Status

abstract class ClientCallListener<RespT>(
    private val delegate: ClientCall.Listener<RespT>,
) : ClientCall.Listener<RespT>() {

    override fun onReady() = delegate.onReady()
    override fun onMessage(message: RespT) = delegate.onMessage(message)
    override fun onHeaders(headers: Metadata?) = delegate.onHeaders(headers)
    override fun onClose(status: Status?, trailers: Metadata?) =
        delegate.onClose(status, trailers)
}