package com.hwx.chatique.p2p

import android.util.Base64
import android.util.Log
import com.hwx.chatique.Configuration
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.models.MessageMetaEvent
import com.hwx.chatique.network.models.MessageMetaType
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.SecretKeySpec

interface IDhCoordinator {
    fun doDhStep1(
        chatId: String,
        isGroup: Boolean,
        targetUserId: Long? = null,
    )

    fun doDhStep2(
        request: MessageMetaEvent,
    )

    fun doDhStep3(
        request: MessageMetaEvent,
    )
}

class DhCoordinator(
    private val streamManager: IStreamManager,
    private val profileHolder: IProfileHolder,
    private val remoteKeys: HashMap<Pair<String, Long>, SecretKeySpec>,
    private val groupMembersRemoteKeys: HashMap<Pair<String, Long>, SecretKeySpec>,
    private val increaseKeyVersion: (String) -> Long,
    private val updateStoredKeys: () -> Unit,
    private val onGroupSingleDhCompleted: (String, Long) -> Unit,
) : IDhCoordinator {

    //dh storing maps:
    private val keyAgreementMap = HashMap<String, KeyAgreement>()

    override fun doDhStep1(chatId: String, isGroup: Boolean, targetUserId: Long?) {
        //DH.pt1. key exchange
        try {
            val aliceKpairGen = KeyPairGenerator.getInstance("DH")
            aliceKpairGen.initialize(Configuration.DH_KEY_SIZE)
            val aliceKpair = aliceKpairGen.generateKeyPair()

            // Alice creates and initializes her DH KeyAgreement object
            val aliceKeyAgree = KeyAgreement.getInstance("DH")
            aliceKeyAgree.init(aliceKpair.private)
            keyAgreementMap[chatId] = aliceKeyAgree

            // Alice encodes her public key, and sends it over to Bob.
            val alicePubKeyEnc = aliceKpair.public.encoded
            val encodedPublicKey = Base64.encodeToString(alicePubKeyEnc, Base64.DEFAULT)

            val requestType = if (isGroup)
                MessageMetaType.GROUP_WELCOME_HANDSHAKE_REQUEST
            else
                MessageMetaType.WELCOME_HANDSHAKE_REQUEST

            //sending response
            val event = MessageMetaEvent(
                UUID.randomUUID().toString(),
                targetUserId?.toString() ?: "",
                chatId,
                requestType,
                encodedPublicKey,
                Date().time / 1000,
                profileHolder.userId,
            )

            streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)
        } catch (ex: Exception) {
            Log.e("AVX", "err on DH alg", ex)
        }
    }

    override fun doDhStep2(request: MessageMetaEvent) {
        //dh.pt2
        try {
            val alicePubKeyEnc = Base64.decode(request.value, Base64.DEFAULT)
            val bobKeyFac = KeyFactory.getInstance("DH")
            val x509KeySpec = X509EncodedKeySpec(alicePubKeyEnc)
            val alicePubKey = bobKeyFac.generatePublic(x509KeySpec)

            /*
                 * Bob gets the DH parameters associated with Alice's public key.
                 * He must use the same parameters when he generates his own key
                 * pair.
                 */
            val dhParamFromAlicePubKey = (alicePubKey as DHPublicKey).params

            // Bob creates his own DH key pair
            val bobKpairGen = KeyPairGenerator.getInstance("DH")
            bobKpairGen.initialize(dhParamFromAlicePubKey)
            val bobKpair = bobKpairGen.generateKeyPair()

            // Bob creates and initializes his DH KeyAgreement object
            val bobKeyAgree = KeyAgreement.getInstance("DH")
            bobKeyAgree.init(bobKpair.private)
            bobKeyAgree.doPhase(alicePubKey, true)

            // Bob encodes his public key, and sends it over to Alice.
            val bobPubKeyEnc = bobKpair.public.encoded
            val base64BobPublicKey = Base64.encodeToString(bobPubKeyEnc, Base64.DEFAULT)

            val bobSharedSecret = bobKeyAgree.generateSecret()
            val bobAesKey = SecretKeySpec(bobSharedSecret, 0, 16, "AES")

            val responseType = if (request.type == MessageMetaType.WELCOME_HANDSHAKE_REQUEST)
                MessageMetaType.WELCOME_HANDSHAKE_RESPONSE
            else
                MessageMetaType.GROUP_WELCOME_HANDSHAKE_RESPONSE

            val newObjectId = if (request.objectId.isEmpty()) "" else request.userFromId.toString()

            //sending response
            val event = MessageMetaEvent(
                UUID.randomUUID().toString(),
                newObjectId,
                request.chatId,
                responseType,
                base64BobPublicKey,
                Date().time / 1000,
                profileHolder.userId,
            )

            streamManager.sendMessageToStream(IStreamManager.StreamId.META_EVENTS, event)

            if (request.type == MessageMetaType.WELCOME_HANDSHAKE_REQUEST) {
                val keyVersion = increaseKeyVersion(request.chatId)
                val keyKey = request.chatId to keyVersion
                remoteKeys[keyKey] = bobAesKey

                Log.w(
                    "AVX",
                    "saving key = ${String(bobAesKey.encoded)} for dual keyKey = $keyKey"
                )
            } else {
                val keyKey = request.chatId to request.userFromId
                groupMembersRemoteKeys[keyKey] = bobAesKey

                Log.w(
                    "AVX",
                    "saving key = ${String(bobAesKey.encoded)} for group keyKey = $keyKey"
                )
            }

            updateStoredKeys()
            //availableKeys.value = remoteKeys.keys.toList()
            Log.w("AVX", "DH2 completed, shared key = " + StringUtils.toHexString(bobSharedSecret))


        } catch (e: Exception) {
            Log.e("AVX", "err on DH2", e)
        }
    }

    override fun doDhStep3(request: MessageMetaEvent) {
        Log.w("AVX2", "doDhStep3")

        //dh.pt3
        /*
         * Alice uses Bob's public key for the first (and only) phase
         * of her version of the DH
         * protocol.
         * Before she can do so, she has to instantiate a DH public key
         * from Bob's encoded key material.
         */
        try {
            val bobPubKeyEnc: ByteArray = Base64.decode(request.value, Base64.DEFAULT)
            val aliceKeyFac = KeyFactory.getInstance("DH")
            val x509KeySpec = X509EncodedKeySpec(bobPubKeyEnc)
            val bobPubKey = aliceKeyFac.generatePublic(x509KeySpec)
            Log.w("AVX", "ALICE: Execute PHASE1 ...")
            val aliceKeyAgree = keyAgreementMap[request.chatId]
            aliceKeyAgree!!.doPhase(bobPubKey, true)
            val aliceSharedSecret = aliceKeyAgree.generateSecret()
            val aliceAesKey = SecretKeySpec(aliceSharedSecret, 0, 16, "AES")

            if (request.type == MessageMetaType.WELCOME_HANDSHAKE_RESPONSE) {
                val keyVersion = increaseKeyVersion(request.chatId)
                val keyKey = request.chatId to keyVersion
                remoteKeys[keyKey] = aliceAesKey
                Log.w(
                    "AVX",
                    "saving key = ${String(aliceAesKey.encoded)} for dual keyKey = $keyKey"
                )
                updateStoredKeys()
            } else {
                //group case
                val keyKey = request.chatId to request.userFromId
                groupMembersRemoteKeys[keyKey] = aliceAesKey
                Log.w(
                    "AVX",
                    "saving key = ${String(aliceAesKey.encoded)} for group keyKey = $keyKey"
                )
                onGroupSingleDhCompleted(request.chatId, request.userFromId)
            }
            Log.w(
                "AVX",
                "DH3 completed, shared key = " + StringUtils.toHexString(aliceSharedSecret)
            )

        } catch (e: java.lang.Exception) {
            Log.e("AVX", "err DH3", e)
        }
    }
}