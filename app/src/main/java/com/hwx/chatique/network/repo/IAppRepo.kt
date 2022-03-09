package com.hwx.chatique.network.repo

import CustomerGrpc.CustomService
import com.hwx.chatique.network.RequestType.grpc
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.grpc.IGrpcStubsHolder
import com.hwx.chatique.network.models.*

interface IAppRepo {

    suspend fun signUp(request: SignUpRequest): Result<EmptyMessage>

    suspend fun signIn(request: SignInRequest): Result<SignInResponse>

    suspend fun getFriendsList(): Result<FriendsListResponse>

    suspend fun getFriendshipRequests(): Result<FriendshipRequestsResponse>

    suspend fun searchUser(searchStr: String): Result<SearchUserResponse>

    suspend fun requestFriendship(userId: Long): Result<EmptyMessage>

    suspend fun handleFriendshipRequest(request: HandleFriendshipRequestRequest): Result<EmptyMessage>

    suspend fun removeFriend(userId: Long): Result<EmptyMessage>

    suspend fun setPushToken(token: String): Result<EmptyMessage>

    //chat
    suspend fun createChat(request: CreateChatRequest): Result<CreateChatResponse>

    suspend fun getChatHistory(request: ChatHistoryRequest): Result<ChatHistoryResponse>

    suspend fun getChats(): Result<ChatsResponse>

    suspend fun getChatInfo(chatId: String): Result<ChatResponseItem>
}

class AppRepo(
    private val holder: IGrpcStubsHolder,
) : IAppRepo {

    override suspend fun signUp(request: SignUpRequest) = grpc {
        val request = CustomService.SignUpRequest.newBuilder()
            .setUsername(request.username)
            .setPassword(request.password)
            .setBio(request.bio)
            .setEmail(request.email)
            .build()
        holder.primary.signUp(request)
        EmptyMessage
    }

    override suspend fun signIn(request: SignInRequest) = grpc {
        val request = CustomService.SignInRequest.newBuilder()
            .setUsername(request.username)
            .setPassword(request.password)
            .build()
        SignInResponse.of(holder.primary.signIn(request))
    }

    override suspend fun getFriendsList() = grpc {
        val request = CustomService.EmptyMessage.newBuilder().build()
        FriendsListResponse.of(holder.primary.getFriends(request))
    }

    override suspend fun getFriendshipRequests() = grpc {
        val request = CustomService.EmptyMessage.newBuilder().build()
        FriendshipRequestsResponse.of(holder.primary.getFriendshipRequests(request))
    }

    override suspend fun searchUser(searchStr: String) = grpc {
        val request = CustomService.SearchUserRequest.newBuilder().setUsername(searchStr).build()
        SearchUserResponse.of(holder.primary.searchUser(request))
    }

    override suspend fun requestFriendship(userId: Long) = grpc {
        val request =
            CustomService.RequestFriendshipRequest.newBuilder().setId(userId.toInt()).build()
        holder.primary.requestFriendship(request)
        EmptyMessage
    }

    override suspend fun handleFriendshipRequest(request: HandleFriendshipRequestRequest) = grpc {
        val request =
            CustomService.HandleFriendshipRequest.newBuilder()
                .setId(request.id)
                .setType(request.type.toGrpc())
                .build()
        holder.primary.handleFriendship(request)
        EmptyMessage
    }

    override suspend fun removeFriend(userId: Long): Result<EmptyMessage> = grpc {
        val request = CustomService.RemoveFriendRequest.newBuilder().setId(userId.toInt()).build()
        holder.primary.removeFriend(request)
        EmptyMessage
    }

    override suspend fun setPushToken(token: String) = grpc {
        val request = SetPushTokenRequest(token)
        holder.primary.setPushToken(request.toGrpc())
        EmptyMessage
    }

    override suspend fun createChat(request: CreateChatRequest) = grpc {
        CreateChatResponse.of(holder.primary.createChat(request.toGrpc()))
    }

    override suspend fun getChatHistory(request: ChatHistoryRequest) = grpc {
        ChatHistoryResponse.of(holder.primary.getChatHistory(request.toGrpc()))
    }

    override suspend fun getChats() = grpc {
        val request = CustomService.EmptyMessage.newBuilder().build()
        ChatsResponse.of(holder.primary.getChats(request))
    }

    override suspend fun getChatInfo(chatId: String) = grpc {
        val request = ChatIdRequest(chatId)
        ChatResponseItem.of(holder.primary.getChatInfo(request.toGrpc()))
    }
}