package com.hwx.chatique.p2p

import android.util.Base64
import android.util.Log
import com.hwx.chatique.Configuration
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptorDecryptor {

    private val iv = IvParameterSpec(Configuration.AES_INIT_VECTOR.toByteArray(Charsets.UTF_8))
    private const val cipherTransformation = "AES/CBC/PKCS5Padding"

    fun encryptMessage(inputBytes: ByteArray, secretKeySpec: SecretKeySpec): String {
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)
        val encryptedBytes = cipher.doFinal(inputBytes)
        val result = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        Log.w(
            "AVX",
            "encrypting (${String(inputBytes)}) with key = ${String(secretKeySpec.encoded)} = $result"
        )
        return result
    }

    fun decryptMessage(inputString: String, secretKeySpec: SecretKeySpec): ByteArray? {
        return try {
            val encodedBytes = Base64.decode(inputString, Base64.DEFAULT)
            val cipher = Cipher.getInstance(cipherTransformation)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv)
            val result = cipher.doFinal(encodedBytes)
            Log.w(
                "AVX",
                "decrypting with key = ${String(secretKeySpec.encoded)} = ${String(result)}"
            )
            result
        } catch (e: Exception) {
            null
        }
    }
}