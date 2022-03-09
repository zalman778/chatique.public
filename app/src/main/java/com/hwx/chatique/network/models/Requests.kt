package com.hwx.chatique.network.models

data class SignUpRequest(
    val username: String,
    val password: String,
    val bio: String,
    val email: String,
)

data class SignInRequest(
    val username: String,
    val password: String,
)

enum class HandleFriendshipRequestType {
    ACCEPT,
    DECLINE;

    fun toGrpc() = when (this) {
        ACCEPT -> CustomerGrpc.CustomService.HandleFriendshipRequestType.ACCEPT
        DECLINE -> CustomerGrpc.CustomService.HandleFriendshipRequestType.DECLINE
    }
}

data class HandleFriendshipRequestRequest(
    val id: String,
    val type: HandleFriendshipRequestType,
)

data class CreateChatRequest(
    val type: ChatResponseItemType = ChatResponseItemType.OPEN,
    val members: List<Long> = emptyList(),
) {

    fun toGrpc() = CustomerGrpc.CustomService.CreateChatRequest.newBuilder()
        .setType(type.toGrpc())
        .addAllMembers(members.map { it.toInt() })
        .build()
}

data class ChatHistoryRequest(
    val dialogId: String,
    val page: Long,
    val limit: Long,
) {
    fun toGrpc() = CustomerGrpc.CustomService.ChatHistoryRequest.newBuilder()
        .setDialogId(dialogId)
        .setPage(page)
        .setLimit(limit)
        .build()
}

data class SetPushTokenRequest(
    val token: String = "",
) {
    fun toGrpc() = CustomerGrpc.CustomService.SetPushTokenRequest.newBuilder()
        .setToken(token)
        .build()
}

data class ChatIdRequest(
    val id: String = "",
) {
    fun toGrpc() = CustomerGrpc.CustomService.ChatIdRequest.newBuilder()
        .setId(id)
        .build()
}