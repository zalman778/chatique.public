package com.hwx.chatique.ui.chatsList

import com.hwx.chatique.network.models.ChatResponseItem
import com.hwx.chatique.network.models.ChatResponseItemType
import com.hwx.chatique.p2p.EncryptorDecryptor
import com.hwx.chatique.p2p.IE2eController

fun ChatResponseItem.extract(
    dhCoordinator: IE2eController,
): ChatResponseItem {
    val isSecret = type == ChatResponseItemType.SECRET
    val key = dhCoordinator.getRemoteKey(id, lastMessageKeyVersion)
    val newMessageStr = if (!isSecret)
        lastMessage
    else {
        key?.let { key ->
            EncryptorDecryptor.decryptMessage(lastMessage, key)?.let { String(it) }
        } ?: lastMessage
    }
    return copy(lastMessage = newMessageStr)
}