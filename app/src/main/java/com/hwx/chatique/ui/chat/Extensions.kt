package com.hwx.chatique.ui.chat

import com.hwx.chatique.network.models.MessageEvent
import com.hwx.chatique.p2p.EncryptorDecryptor
import com.hwx.chatique.p2p.IE2eController

fun MessageEvent.prepareToSend(
    isSecret: Boolean, dialogId: String, dhCoordinator: IE2eController,
): MessageEvent {
    val newMessageStr = if (!isSecret) {
        value
    } else {
        val key = dhCoordinator.getRemoteKey(dialogId)
        key?.let {
            EncryptorDecryptor.encryptMessage(value.toByteArray(), key)
        } ?: value
    }
    return copy(value = newMessageStr)
}

fun MessageEvent.extract(
    isSecret: Boolean, dialogId: String, dhCoordinator: IE2eController,
): MessageEvent {
    val key = dhCoordinator.getRemoteKey(dialogId, keyVersion)
    val newMessageStr = if (!isSecret)
        value
    else {
        key?.let { key ->
            EncryptorDecryptor.decryptMessage(value, key)?.let { String(it) }
        } ?: value
    }
    return copy(value = newMessageStr)
}