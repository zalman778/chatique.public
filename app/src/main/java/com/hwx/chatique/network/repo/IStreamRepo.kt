package com.hwx.chatique.network.repo

import com.hwx.chatique.network.grpc.IGrpcStubsHolder
import com.hwx.chatique.network.models.MessageEvent
import com.hwx.chatique.network.models.MessageMetaEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface IStreamRepo {
    fun messageEvents(input: Flow<MessageEvent>): Flow<MessageEvent>
    fun metaEvents(input: Flow<MessageMetaEvent>): Flow<MessageMetaEvent>
}

class StreamRepo(
    private val holder: IGrpcStubsHolder,
) : IStreamRepo {

    override fun messageEvents(input: Flow<MessageEvent>) = holder.primary
        .messageEvents(input.map { it.toGrpc() })
        .map { MessageEvent.of(it) }

    override fun metaEvents(input: Flow<MessageMetaEvent>) = holder.primary
        .messageMetaEvents(input.map { it.toGrpc() })
        .map { MessageMetaEvent.of(it) }

}