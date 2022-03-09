package com.hwx.chatique.ui.login


fun test() {

    inlinedFunction {
        print(it)
    }
}

inline fun inlinedFunction(crossinline block: (String) -> Unit) {
    block("test")
    val innerTask = {
        block("test")
    }
    innerTask.invoke()
}