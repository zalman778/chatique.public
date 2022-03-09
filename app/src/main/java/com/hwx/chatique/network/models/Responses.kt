package com.hwx.chatique.network.models

import CustomerGrpc.CustomService
import androidx.annotation.StringRes
import com.hwx.chatique.R

data class FriendsListResponse(
    val items: List<FriendsListItemResponse> = emptyList(),
) {
    companion object {
        fun of(it: CustomService.FriendsResponse) =
            FriendsListResponse(it.itemsList.map { FriendsListItemResponse.of(it) })
    }
}

data class FriendsListItemResponse(
    val id: String = "",
    val username: String = "",
) {
    companion object {
        fun of(it: CustomService.FriendResponseItem) =
            FriendsListItemResponse(it.id, it.username)
    }
}

data class FriendshipRequestsResponse(
    val items: List<FriendshipRequestsItem> = emptyList(),
) {
    companion object {
        fun of(it: CustomService.FriendshipRequestsResponse) =
            FriendshipRequestsResponse(it.itemsList.map { FriendshipRequestsItem.of(it) })
    }
}

data class FriendshipRequestsItem(
    val id: String = "",
    val username: String = "",
    val type: FriendshipRequestType = FriendshipRequestType.INPUT,
) {
    companion object {
        fun of(it: CustomService.FriendshipRequestsItem) =
            FriendshipRequestsItem(it.id, it.username, FriendshipRequestType.of(it.type))
    }
}

enum class FriendshipRequestType(@StringRes val captionResId: Int) {
    INPUT(R.string.input),
    OUTPUT(R.string.output);

    companion object {
        fun of(it: CustomService.FriendshipRequestType) = when (it) {
            CustomService.FriendshipRequestType.INPUT -> INPUT
            CustomService.FriendshipRequestType.OUTPUT -> OUTPUT
            CustomService.FriendshipRequestType.UNRECOGNIZED -> INPUT
        }
    }
}

object EmptyMessage

data class SignInResponse(
    val token: String = "",
    val id: Long = 0L,
    val username: String = "",
) {
    companion object {
        fun of(it: CustomService.SignInResponse) =
            SignInResponse(it.token, it.id.toLong(), it.username)
    }
}

data class SearchUserResponse(
    val items: List<SearchUserResponseItem> = emptyList(),
) {
    companion object {
        fun of(it: CustomService.SearchUserResponse) =
            SearchUserResponse(it.itemsList.map { SearchUserResponseItem.of(it) })
    }
}

data class SearchUserResponseItem(
    val id: Long = 0L,
    val username: String = "",
) {
    companion object {
        fun of(it: CustomService.SearchUserResponseItem) =
            SearchUserResponseItem(it.id.toLong(), it.username)
    }
}

data class CreateChatResponse(
    val id: String = "",
    val membersOnline: List<Long> = emptyList(),
) {
    companion object {
        fun of(it: CustomService.CreateChatResponse) =
            CreateChatResponse(it.id, it.membersOnlineList.map { it.toLong() })
    }
}

data class ChatHistoryResponse(
    val items: List<MessageEvent> = emptyList(),
    val totalPages: Long = 0L,
) {
    companion object {
        fun of(it: CustomService.ChatHistoryResponse) =
            ChatHistoryResponse(it.itemsList.map { MessageEvent.of(it) }, it.totalPages)
    }
}

data class ChatsResponse(
    val items: List<ChatResponseItem> = emptyList(),
) {
    companion object {
        fun of(it: CustomService.ChatsResponse) =
            ChatsResponse(it.itemsList.map { ChatResponseItem.of(it) })
    }
}

data class ChatResponseItem(
    val id: String = "",
    val caption: String = "",
    val type: ChatResponseItemType = ChatResponseItemType.OPEN,
    val groupType: ChatResponseItemGroupType = ChatResponseItemGroupType.DUAL,
    val lastMessage: String = "",
    val lastMessageKeyVersion: Long = -1L,
    val createDate: Long = -1L,
    val members: List<ChatMember> = emptyList(),
) {
    companion object {
        fun of(it: CustomService.ChatResponseItem) = ChatResponseItem(
            it.id, it.caption, ChatResponseItemType.of(it.type),
            ChatResponseItemGroupType.of(it.groupType),
            it.lastMessage, it.lastMessageKeyVersion, it.createDate,
            it.membersList.map { ChatMember.of(it) },
        )

        fun empty() = of(CustomService.ChatResponseItem.newBuilder().build())
    }
}

enum class ChatResponseItemGroupType {
    DUAL,
    GROUP;

    companion object {
        fun of(it: CustomService.ChatResponseItemGroupType) = when (it) {
            CustomService.ChatResponseItemGroupType.DUAL -> DUAL
            else -> GROUP
        }
    }
}

data class ChatMember(
    val userId: Long = -1L,
    val username: String = "",
    val isCommunicating: Boolean = false,
) {
    companion object {
        fun of(it: CustomService.ChatMember) =
            ChatMember(it.userId.toLong(), it.username, it.isCommunicating)
    }
}