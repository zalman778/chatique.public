package com.hwx.chatique.helpers

import androidx.navigation.NavController
import java.lang.ref.WeakReference

interface INavigationHolder {
    val controller: NavController?
    fun init(controller: NavController)
}

class NavigationHolder : INavigationHolder {

    private var weakController: WeakReference<NavController>? = null

    override val controller: NavController?
        get() = weakController?.get()

    override fun init(controller: NavController) {
        weakController = WeakReference(controller)
    }
}