package com.hwx.chatique.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

interface IToaster {

    fun longToast(message: String?)
    fun shortToast(message: String?)
    fun longToast(@StringRes messageId: Int)
    fun shortToast(@StringRes messageId: Int)
}

class ToasterImpl(
    private val context: Context
) : IToaster {

    private val handler = Handler(Looper.getMainLooper())
    private var toastRef: WeakReference<Toast>? = null

    override fun longToast(message: String?) = message?.let { toast(it, true) } ?: Unit
    override fun shortToast(message: String?) = message?.let { toast(it, false) } ?: Unit
    override fun longToast(@StringRes messageId: Int) = toast(context.getString(messageId), true)
    override fun shortToast(@StringRes messageId: Int) = toast(context.getString(messageId), false)

    private fun toast(message: String, isLong: Boolean) {
        if (message.isBlank()) return
        val length = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        if (Looper.myLooper() == Looper.getMainLooper()) show(
            message,
            length
        ) else handler.post { show(message, length) }
    }

    private fun show(message: String, length: Int) = Toast.makeText(context, message, length).also {
        toastRef?.get()?.cancel()
        it.show()
        toastRef = WeakReference(it)
    }
}