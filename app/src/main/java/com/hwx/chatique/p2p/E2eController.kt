package com.hwx.chatique.p2p

import android.util.Base64
import android.util.Log
import com.hwx.chatique.Configuration
import com.hwx.chatique.arch.extensions.equalsToOneOf
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.helpers.IPreferencesStore
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.models.MessageMetaEvent
import com.hwx.chatique.network.models.MessageMetaType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

interface IE2eController {

    val availableKeys: StateFlow<Set<String>>

    fun init()

    fun initServerPart()

    fun startClientFlow(
        chatId: String,
        membersOnline: List<Long>,
        userIds: List<Long>,
    )

    fun getRemoteKey(chatId: String, keyVersion: Long? = null): SecretKeySpec?

    fun getKeyVersion(chatId: String): Long

    fun requestKeySharing(chatId: String, memberIds: List<Long>)

    fun onDestroy()

    fun logout()
}

class E2eController(
    private val streamManager: IStreamManager,
    private val prefs: IPreferencesStore,
    private val profileHolder: IProfileHolder,
) : IE2eController, CoroutineScope {

    private companion object {
        const val STORED_REMOTE_KEYS = "STORED_REMOTE_KEYS"
    }

    data class SerializesRemoteKeys(
        val items: List<Pair<Pair<String, Long>, String>> = emptyList(),
        val keyVersionsItems: List<Pair<String, Long>> = emptyList(),
    )

    data class GroupAdminTask(
        val initialMembersOnline: Set<Long> = emptySet(),
        val targetUserIds: Set<Long> = emptySet(),
        val completedUserIds: Set<Long> = emptySet(),
    )

    data class KeySharingTask(
        val targetUserId: Long = -1L,
    )

    private val innerJob = Job()
    override val coroutineContext = Dispatchers.IO + innerJob

    override val availableKeys = MutableStateFlow(emptySet<String>())

    private val remoteKeys =
        HashMap<Pair<String, Long>, SecretKeySpec>() //key = chatId to keyVersion

    private val groupMembersRemoteKeys =
        HashMap<Pair<String, Long>, SecretKeySpec>() //key = chatId to userId

    private val remoteKeyVersions = HashMap<String, Long>() //current keyVersion for chatId
    private val groupAdminTasks = HashMap<String, GroupAdminTask>()
    private val keySharingTasks = HashMap<String, KeySharingTask>()
    private val timeoutJobs = HashMap<String, Job>()
    private val sharedKeyRequestingJobs = HashMap<String, Job>()

    private val dhCoordinator: IDhCoordinator = DhCoordinator(
        streamManager,
        profileHolder,
        remoteKeys,
        groupMembersRemoteKeys,
        ::increaseKeyVersion,
        ::updateStoredKeys,
        ::onGroupSingleDhCompleted
    )

    override fun init() {
        restoreRemoteKeys()
    }

    override fun initServerPart() {
        launch {
            streamManager
                .getStreamInput<MessageMetaEvent>(IStreamManager.StreamId.META_EVENTS)
                .filter {
                    it.type.equalsToOneOf(
                        MessageMetaType.WELCOME_HANDSHAKE_REQUEST,
                        MessageMetaType.WELCOME_HANDSHAKE_RESPONSE,
                    ) ||
                            (it.type == MessageMetaType.SHARING_SECRET_KEY && it.objectId == profileHolder.userId.toString()) ||
                            (it.type == MessageMetaType.REQUEST_KEY_SHARING && it.objectId == profileHolder.userId.toString()) ||
                            (it.type == MessageMetaType.GROUP_WELCOME_HANDSHAKE_REQUEST && (it.objectId == profileHolder.userId.toString() || it.objectId.isEmpty())) ||
                            (it.type == MessageMetaType.GROUP_WELCOME_HANDSHAKE_RESPONSE && (it.objectId == profileHolder.userId.toString() || it.objectId.isEmpty()))
                }
                .collect {
                    when (it.type) {
                        MessageMetaType.WELCOME_HANDSHAKE_REQUEST,
                        MessageMetaType.GROUP_WELCOME_HANDSHAKE_REQUEST -> dhCoordinator.doDhStep2(
                            it
                        )
                        MessageMetaType.WELCOME_HANDSHAKE_RESPONSE,
                        MessageMetaType.GROUP_WELCOME_HANDSHAKE_RESPONSE -> dhCoordinator.doDhStep3(
                            it
                        )
                        MessageMetaType.SHARING_SECRET_KEY -> receiveGroupSharedKey(it)
                        MessageMetaType.REQUEST_KEY_SHARING -> sendGroupSharedKey(it)
                    }
                }
        }
    }

    private fun sendGroupSharedKey(event: MessageMetaEvent) {
        val sharedKeyKey = event.chatId to 0L
        val sharedKey = remoteKeys[sharedKeyKey] ?: return
        val memberKeyKey = event.chatId to event.userFromId
        val memberKey = groupMembersRemoteKeys[memberKeyKey]
        memberKey?.let {
            val encryptedSharedKey = EncryptorDecryptor.encryptMessage(sharedKey.encoded, memberKey)
            val newEvent = MessageMetaEvent(
                UUID.randomUUID().toString(),
                event.userFromId.toString(),
                event.chatId,
                MessageMetaType.SHARING_SECRET_KEY,
                encryptedSharedKey,
                Date().time / 1000,
                profileHolder.userId,
            )
            streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, newEvent)
        }
    }

    private fun receiveGroupSharedKey(it: MessageMetaEvent) {
        val keyKey = it.chatId to it.userFromId
        val storedKey = groupMembersRemoteKeys[keyKey] ?: return
        val sharedKey = EncryptorDecryptor.decryptMessage(it.value, storedKey) ?: return
        remoteKeys[it.chatId to 0L] = SecretKeySpec(sharedKey, 0, 16, "AES")
        updateStoredKeys()
    }


    override fun startClientFlow(
        chatId: String,
        membersOnline: List<Long>,
        userIds: List<Long>,
    ) {
        val isGroup = userIds.size > 1
        dhCoordinator.doDhStep1(chatId, isGroup)

        if (isGroup) {
            generateGroupSharedKey(chatId)
            groupAdminTasks[chatId] = GroupAdminTask(membersOnline.toSet(), userIds.toSet())
            doGroupAdminPart(chatId)
        }
    }

    private fun generateGroupSharedKey(chatId: String) {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(Configuration.DH_KEY_SIZE)
        val sharedKey = generator.generateKey()
        val keySpec = SecretKeySpec(sharedKey.encoded, 0, 16, "AES")
        val keyKey = chatId to 0L
        remoteKeys[keyKey] = keySpec
        updateStoredKeys()
    }

    private fun doGroupAdminPart(chatId: String) {
        //here we should await for completion of DH of every online member (WITH timeout = 3s)
        timeoutJobs[chatId] = GlobalScope.launch {
            delay(3000L)
            onAllOnlineGroupMembersDhCompleted(chatId)
        }
    }

    private fun onGroupSingleDhCompleted(chatId: String, userFromId: Long) {
        checkGroupAdminTasks(chatId, userFromId)
        checkKeySharingTasks(chatId, userFromId)
    }

    private fun checkGroupAdminTasks(chatId: String, userFromId: Long) {
        val task = groupAdminTasks[chatId] ?: return
        val newCompletedSet = task.completedUserIds.toMutableSet().apply {
            add(userFromId)
        }
        groupAdminTasks[chatId] = task.copy(completedUserIds = newCompletedSet)
        if (task.completedUserIds == task.initialMembersOnline) {
            timeoutJobs[chatId]?.cancel()
            onAllOnlineGroupMembersDhCompleted(chatId)
        }
    }

    private fun checkKeySharingTasks(chatId: String, userFromId: Long) {
        val task = keySharingTasks[chatId] ?: return
        if (task.targetUserId != userFromId) return
        //if dh3 completed with that user, then we should send shared key request
        val keyKey = chatId to userFromId
        val hasKey = groupMembersRemoteKeys.containsKey(keyKey)
        if (!hasKey) return
        //sending response
        val event = MessageMetaEvent(
            UUID.randomUUID().toString(),
            userFromId.toString(),
            chatId,
            MessageMetaType.REQUEST_KEY_SHARING,
            "",
            Date().time / 1000,
            profileHolder.userId,
        )
        streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
    }

    //we should generate new private key for group and share it by encrypted 1-1 channels
    private fun onAllOnlineGroupMembersDhCompleted(chatId: String) {
        val task = groupAdminTasks[chatId] ?: return
        val keyKey = chatId to 0L
        val sharedKey = remoteKeys[keyKey] ?: return

        //encoding key
        task.initialMembersOnline.forEach { targetUserId ->
            val memberKeyKey = chatId to targetUserId
            val memberKey = groupMembersRemoteKeys[memberKeyKey]
            memberKey?.let {
                val encryptedSharedKey =
                    EncryptorDecryptor.encryptMessage(sharedKey.encoded, memberKey)
                //sending response
                val event = MessageMetaEvent(
                    UUID.randomUUID().toString(),
                    targetUserId.toString(),
                    chatId,
                    MessageMetaType.SHARING_SECRET_KEY,
                    encryptedSharedKey,
                    Date().time / 1000,
                    profileHolder.userId,
                )
                streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
            }
        }

        groupAdminTasks.remove(chatId)
    }

    override fun getRemoteKey(chatId: String, keyVersion: Long?): SecretKeySpec? {
        val keyVersion = keyVersion ?: getKeyVersion(chatId)
        val result = remoteKeys[chatId to keyVersion]
        Log.w("AVX", "getRemoteKey($chatId $keyVersion) = ${result?.encoded?.let { String(it) }}")
        return result
    }

    override fun getKeyVersion(chatId: String) = remoteKeyVersions[chatId] ?: 0L

    override fun requestKeySharing(chatId: String, memberIds: List<Long>) {
        //выбираем рандомного пользователя чата, делаем с ним обмен ключами и запрашиваем общий ключ
        //если не отвечает, то ждем 3 сек, выбираем следующего

        sharedKeyRequestingJobs[chatId] = GlobalScope.launch {
            for (userId in memberIds.filter { it != profileHolder.userId }) {
                Log.w("AVX", "requestKeySharing chatId = $chatId userId = $userId")
                keySharingTasks[chatId] = KeySharingTask(userId)
                dhCoordinator.doDhStep1(chatId, true, userId)
                delay(4000)
                keySharingTasks.remove(chatId)
            }
        }
    }

    override fun onDestroy() {
        innerJob.cancelChildren()
    }

    private fun updateStoredKeys() {
        val list = remoteKeys
            .mapValues { Base64.encodeToString(it.value.encoded, Base64.DEFAULT) }
            .toList()
        val keysList = remoteKeyVersions.toList()
        prefs.store(STORED_REMOTE_KEYS, SerializesRemoteKeys(list, keysList))
        availableKeys.value = remoteKeys.keys.map { it.first }.toSet()
    }

    private fun restoreRemoteKeys() {
        val restored =
            prefs.restore(STORED_REMOTE_KEYS, SerializesRemoteKeys::class.java) ?: return
        restored.items.forEach {
            val secret = Base64.decode(it.second, Base64.DEFAULT)
            val spec = SecretKeySpec(secret, 0, 16, "AES")
            remoteKeys[it.first] = spec
        }
        remoteKeyVersions.clear()
        restored.keyVersionsItems.forEach {
            remoteKeyVersions[it.first] = it.second
        }
        Log.w("AVX", "restored: $remoteKeys")
        availableKeys.value = remoteKeys.keys.map { it.first }.toSet()
    }

    private fun increaseKeyVersion(chatId: String): Long {
        val newVersion = remoteKeyVersions[chatId]?.let { it + 1 } ?: 0L
        remoteKeyVersions[chatId] = newVersion
        return newVersion
    }

    override fun logout() {
        prefs.removeByKey(STORED_REMOTE_KEYS)
    }
}