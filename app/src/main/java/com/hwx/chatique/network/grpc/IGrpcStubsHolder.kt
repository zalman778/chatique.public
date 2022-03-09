package com.hwx.chatique.network.grpc

import CustomerGrpc.CustomerServiceGrpcKt
import io.grpc.Channel

interface IGrpcStubsHolder {
    val primary: CustomerServiceGrpcKt.CustomerServiceCoroutineStub
}

class GrpcStubsHolder(
    channel: Channel
) : IGrpcStubsHolder {

    override val primary: CustomerServiceGrpcKt.CustomerServiceCoroutineStub by lazy {
        CustomerServiceGrpcKt.CustomerServiceCoroutineStub(channel)
    }
}