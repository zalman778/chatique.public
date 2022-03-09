package com.hwx.chatique.network.models

import com.google.protobuf.ByteString

enum class ChatResponseItemType {
    OPEN,
    SECRET;

    fun toGrpc() = when (this) {
        OPEN -> CustomerGrpc.CustomService.ChatResponseItemType.OPEN
        SECRET -> CustomerGrpc.CustomService.ChatResponseItemType.SECRET
    }

    companion object {
        fun of(it: CustomerGrpc.CustomService.ChatResponseItemType) = when (it) {
            CustomerGrpc.CustomService.ChatResponseItemType.SECRET -> SECRET
            else -> OPEN
        }
    }
}

data class MessageEvent(
    val id: String = "",
    val dialogId: String = "",
    val userFromId: Long = -1L,
    val value: String = "",
    val createDate: Long = -1L,
    val keyVersion: Long = -1L,
    val userFrom: String = "",
) {

    fun toGrpc() = CustomerGrpc.CustomService.MessageEvent.newBuilder()
        .setId(id)
        .setDialogId(dialogId)
        .setUserFromId(userFromId.toInt())
        .setValue(value)
        .setCreateDate(createDate)
        .setKeyVersion(keyVersion)
        .setUserFrom(userFrom)
        .build()

    companion object {
        fun of(it: CustomerGrpc.CustomService.MessageEvent) =
            MessageEvent(
                it.id,
                it.dialogId,
                it.userFromId.toLong(),
                it.value,
                it.createDate,
                it.keyVersion,
                it.userFrom,
            )
    }
}

data class MessageMetaEvent(
    val id: String = "",
    val objectId: String = "",
    val chatId: String = "",
    val type: MessageMetaType = MessageMetaType.UNRECOGNIZED,
    val value: String = "",
    val createDate: Long = -1L,
    val userFromId: Long = -1L,
    val bytesPayload: ByteArray = ByteArray(0), //todo - may break something...
) {

    fun toGrpc() = CustomerGrpc.CustomService.MessageMetaEvent.newBuilder()
        .setId(id)
        .setObjectId(objectId)
        .setChatId(chatId)
        .setValue(value)
        .setType(type.toGrpc())
        .setCreateDate(createDate)
        .setUserFromId(userFromId)
        .setBytesPayload(ByteString.copyFrom(bytesPayload))
        .build()

    companion object {
        fun of(it: CustomerGrpc.CustomService.MessageMetaEvent) =
            MessageMetaEvent(
                it.id,
                it.objectId,
                it.chatId,
                MessageMetaType.of(it.type),
                it.value,
                it.createDate,
                it.userFromId,
                it.bytesPayload.toByteArray(),
            )
    }
}

enum class MessageMetaType {
    WELCOME_HANDSHAKE_REQUEST,
    WELCOME_HANDSHAKE_RESPONSE,
    GROUP_WELCOME_HANDSHAKE_REQUEST,
    GROUP_WELCOME_HANDSHAKE_RESPONSE,
    REQUEST_KEY_SHARING,
    SHARING_SECRET_KEY,

    //call
    REQUEST_VOICE_CALL,
    VOICE_CALL_RESPONSE,
    VOICE_CALL_PAYLOAD,
    VIDEO_CALL_PAYLOAD,

    MESSAGE_EDITED,
    MESSAGE_REMOVED,
    TYPING_MESSAGE,
    UNRECOGNIZED;

    fun toGrpc() = when (this) {
        WELCOME_HANDSHAKE_REQUEST -> CustomerGrpc.CustomService.MessageMetaType.WELCOME_HANDSHAKE_REQUEST
        WELCOME_HANDSHAKE_RESPONSE -> CustomerGrpc.CustomService.MessageMetaType.WELCOME_HANDSHAKE_RESPONSE
        GROUP_WELCOME_HANDSHAKE_REQUEST -> CustomerGrpc.CustomService.MessageMetaType.GROUP_WELCOME_HANDSHAKE_REQUEST
        GROUP_WELCOME_HANDSHAKE_RESPONSE -> CustomerGrpc.CustomService.MessageMetaType.GROUP_WELCOME_HANDSHAKE_RESPONSE
        REQUEST_KEY_SHARING -> CustomerGrpc.CustomService.MessageMetaType.REQUEST_KEY_SHARING
        SHARING_SECRET_KEY -> CustomerGrpc.CustomService.MessageMetaType.SHARING_SECRET_KEY
        REQUEST_VOICE_CALL -> CustomerGrpc.CustomService.MessageMetaType.REQUEST_VOICE_CALL
        VOICE_CALL_RESPONSE -> CustomerGrpc.CustomService.MessageMetaType.VOICE_CALL_RESPONSE
        VOICE_CALL_PAYLOAD -> CustomerGrpc.CustomService.MessageMetaType.VOICE_CALL_PAYLOAD
        VIDEO_CALL_PAYLOAD -> CustomerGrpc.CustomService.MessageMetaType.VIDEO_CALL_PAYLOAD
        MESSAGE_EDITED -> CustomerGrpc.CustomService.MessageMetaType.MESSAGE_EDITED
        MESSAGE_REMOVED -> CustomerGrpc.CustomService.MessageMetaType.MESSAGE_REMOVED
        TYPING_MESSAGE -> CustomerGrpc.CustomService.MessageMetaType.TYPING_MESSAGE
        UNRECOGNIZED -> CustomerGrpc.CustomService.MessageMetaType.UNRECOGNIZED
    }

    companion object {
        fun of(it: CustomerGrpc.CustomService.MessageMetaType) = when (it) {
            CustomerGrpc.CustomService.MessageMetaType.WELCOME_HANDSHAKE_REQUEST -> WELCOME_HANDSHAKE_REQUEST
            CustomerGrpc.CustomService.MessageMetaType.WELCOME_HANDSHAKE_RESPONSE -> WELCOME_HANDSHAKE_RESPONSE
            CustomerGrpc.CustomService.MessageMetaType.GROUP_WELCOME_HANDSHAKE_REQUEST -> GROUP_WELCOME_HANDSHAKE_REQUEST
            CustomerGrpc.CustomService.MessageMetaType.GROUP_WELCOME_HANDSHAKE_RESPONSE -> GROUP_WELCOME_HANDSHAKE_RESPONSE
            CustomerGrpc.CustomService.MessageMetaType.REQUEST_KEY_SHARING -> REQUEST_KEY_SHARING
            CustomerGrpc.CustomService.MessageMetaType.REQUEST_VOICE_CALL -> REQUEST_VOICE_CALL
            CustomerGrpc.CustomService.MessageMetaType.VOICE_CALL_RESPONSE -> VOICE_CALL_RESPONSE
            CustomerGrpc.CustomService.MessageMetaType.VOICE_CALL_PAYLOAD -> VOICE_CALL_PAYLOAD
            CustomerGrpc.CustomService.MessageMetaType.VIDEO_CALL_PAYLOAD -> VIDEO_CALL_PAYLOAD
            CustomerGrpc.CustomService.MessageMetaType.SHARING_SECRET_KEY -> SHARING_SECRET_KEY
            CustomerGrpc.CustomService.MessageMetaType.MESSAGE_EDITED -> MESSAGE_EDITED
            CustomerGrpc.CustomService.MessageMetaType.MESSAGE_REMOVED -> MESSAGE_REMOVED
            CustomerGrpc.CustomService.MessageMetaType.TYPING_MESSAGE -> TYPING_MESSAGE
            else -> UNRECOGNIZED
        }
    }
}

