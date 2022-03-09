package com.hwx.chatique.helpers

import androidx.compose.material.SnackbarHostState
import com.hwx.chatique.network.Result
import io.grpc.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

interface ISnackbarManager {
    var host: SnackbarHostState?
    var scope: CoroutineScope?
    fun showMessage(message: String)
}

class SnackbarManager : ISnackbarManager {

    private var weakHost: WeakReference<SnackbarHostState>? = null
    private var weakScope: WeakReference<CoroutineScope>? = null

    override var host: SnackbarHostState?
        get() = weakHost?.get()
        set(value) {
            weakHost = WeakReference(value)
        }

    override var scope: CoroutineScope?
        get() = weakScope?.get()
        set(value) {
            weakScope = WeakReference(value)
        }

    override fun showMessage(message: String) {
        val host = weakHost?.get() ?: return
        val scope = weakScope?.get() ?: return
        scope.launch {
            host.showSnackbar(message)
        }
    }
}

fun ISnackbarManager.tryExtractError(response: Result.Fail) {
    val status = Status.fromThrowable(response.cause)
    val errStr = status.description ?: "Error, please, retry later."
    showMessage(errStr)
}