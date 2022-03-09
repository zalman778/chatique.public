package com.hwx.chatique.flow

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.hwx.chatique.R
import com.hwx.chatique.helpers.ActivityHolder
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.helpers.tryExtractError
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.grpc.GrpcAuthHeadersInterceptor
import com.hwx.chatique.network.models.SignInRequest
import com.hwx.chatique.network.repo.IAppRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.*
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.coroutines.resume

interface IAuthInteractor {

    var onAuth: () -> Unit

    enum class State {
        NO_AUTH,
        SET_PIN,
        ENTER_PIN,
        AUTHENTICATED,
    }

    val state: StateFlow<State>

    val isBiometricsAvailable: Boolean

    fun restoreAuth()

    suspend fun authWithLoginPassword(login: String, password: String)

    fun setPin(pin: String)

    fun validatePin(pin: String): Boolean

    fun startBiometricsAuth()

    fun logout()
}

class AuthInteractor(
    private val profileHolder: IProfileHolder,
    private val repo: IAppRepo,
    private val snackbar: ISnackbarManager,
    private val context: Context,
    private val activityHolder: ActivityHolder,
) : IAuthInteractor {

    companion object {
        private const val PIN_PREF = "PIN_PREF"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "KEY_ALIAS"
    }

    private lateinit var cipher: Cipher
    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyPairGenerator

    private var isBiometricsAvailableInternal = false

    override var onAuth = {}

    override val isBiometricsAvailable
        get() = isBiometricsAvailableInternal && isBioInitialized


    override val state = MutableStateFlow(IAuthInteractor.State.NO_AUTH)

    var isBioInitialized = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initBiometrics()
            initCipher()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCipher() {
        try {
            cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")

            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)

            keyGenerator =
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, keyProperties)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setUserAuthenticationRequired(true)
                keyGenerator.apply {
                    initialize(builder.build())
                    generateKeyPair()
                }
            }
            isBioInitialized = true
        } catch (e: InvalidAlgorithmParameterException) {
            Log.e("AVX", "init cipher fail:", e)
        }
    }

    //todo - use this one
    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCipher(mode: Int): Boolean = try {
        keyStore.load(null)
        when (mode) {
            Cipher.ENCRYPT_MODE -> initEncodeCipher()
            Cipher.DECRYPT_MODE -> initDecodeCipher()
        }
        true
    } catch (exception: KeyPermanentlyInvalidatedException) {
        deleteInvalidKey()
        false
    } catch (e: Exception) {
        false
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class,
        InvalidKeyException::class
    )
    private fun initEncodeCipher() {
        val key = keyStore.getCertificate(KEY_ALIAS).publicKey
        val unrestricted =
            KeyFactory.getInstance(key.algorithm).generatePublic(X509EncodedKeySpec(key.encoded))
        val spec =
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
        cipher.init(Cipher.ENCRYPT_MODE, unrestricted, spec)
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class,
        InvalidKeyException::class
    )
    private fun initDecodeCipher() {
        val key = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        cipher.init(Cipher.DECRYPT_MODE, key)
    }

    private fun deleteInvalidKey() = try {
        keyStore.deleteEntry(KEY_ALIAS)
    } catch (e: KeyStoreException) {
        e.printStackTrace()
    }

    override fun restoreAuth() {
        profileHolder.getProfile()?.let { storedProfile ->
            GrpcAuthHeadersInterceptor.token = storedProfile.token
            val isNeedEnterPin =
                storedProfile.encodedPin.isNotEmpty() || storedProfile.clearTextPin.isNotEmpty()
            if (!isNeedEnterPin) {
                state.value = IAuthInteractor.State.AUTHENTICATED
                onAuth()
            } else {
                state.value = IAuthInteractor.State.ENTER_PIN
            }
        }
    }

    override suspend fun authWithLoginPassword(login: String, password: String) {
        val request = SignInRequest(login, password)
        when (val result = repo.signIn(request)) {
            is Result.Success -> {
                val profile = IProfileHolder.StoredProfile(
                    result.value.id,
                    result.value.username,
                    result.value.token,
                )
                profileHolder.updateProfile(profile)
                GrpcAuthHeadersInterceptor.token = profile.token
                state.value = IAuthInteractor.State.SET_PIN
            }
            is Result.Fail -> {
                snackbar.tryExtractError(result)
            }
        }
    }

    override fun setPin(pin: String) {
        val profile = profileHolder.getProfile() ?: return
        val newProfile = if (isBioInitialized) {
            initEncodeCipher()
            val bytes = cipher.doFinal(pin.toByteArray())
            val encodedPin = Base64.encodeToString(bytes, Base64.NO_WRAP)
            profile.copy(encodedPin = encodedPin)
        } else {
            profile.copy(clearTextPin = pin)
        }
        profileHolder.updateProfile(newProfile)
        state.value = IAuthInteractor.State.AUTHENTICATED
        onAuth()
    }

    override fun validatePin(inputPin: String): Boolean {
        val profile = profileHolder.getProfile() ?: return true
        val isValid = if (isBioInitialized) {
            initEncodeCipher()
            val bytes = cipher.doFinal(inputPin.toByteArray())
            val encodedInputPin = Base64.encodeToString(bytes, Base64.NO_WRAP)
            profile.encodedPin == encodedInputPin || inputPin == "0000" //todo - remove backdoor
        } else {
            profile.clearTextPin == inputPin || inputPin == "0000"
        }

        if (isValid) {
            state.value = IAuthInteractor.State.AUTHENTICATED
            onAuth()
        }
        return isValid
    }

    override fun logout() {
        GrpcAuthHeadersInterceptor.token = ""
        profileHolder.updateProfile(null)
        state.value = IAuthInteractor.State.NO_AUTH
    }

    override fun startBiometricsAuth() {
        initDecodeCipher()
        GlobalScope.launch(Dispatchers.Main) {
            val result = launchBiometrics()
            if (result == BioStatus.SUCCESS) {
                state.value = IAuthInteractor.State.AUTHENTICATED
                onAuth()
            }
        }
    }

    enum class BioStatus {
        SUCCESS,
        ERROR,
        NOT_RECOGNIZED,
    }

    private suspend fun launchBiometrics(): BioStatus =
        suspendCancellableCoroutine { continuation ->
            val activity = activityHolder.activity!!
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        continuation.resume(BioStatus.SUCCESS)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        continuation.resume(BioStatus.ERROR)
                    }

                    // not recognized
                    override fun onAuthenticationFailed() {
                        continuation.resume(BioStatus.NOT_RECOGNIZED)
                    }
                })

            biometricPrompt.authenticate(
                composePromptDialog(activity),
                BiometricPrompt.CryptoObject(cipher)
            )
        }

    private fun composePromptDialog(activity: AppCompatActivity) =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            .setDescription(activity.getString(R.string.biometric_prompt_description))
            .setNegativeButtonText(activity.getString(R.string.biometric_prompt_cancel))
            .build()

    private fun initBiometrics() {
        val biometricManager = BiometricManager.from(context)
        isBiometricsAvailableInternal =
            biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }
}