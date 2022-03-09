package com.hwx.chatique.helpers

import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

class ActivityHolder {

    private var reference: WeakReference<AppCompatActivity?> = WeakReference(null)

    var activity: AppCompatActivity?
        get() = reference.get()
        set(value) {
            reference = WeakReference(value)
        }
}