package com.hwx.chatique.ui.login

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState

interface LoginContract {

    data class State(
        val login: String = "",
        val password: String = "",
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        data class LoginChanged(val value: String) : Event
        data class PasswordChanged(val value: String) : Event
        object OnLoginClick : Event
    }

    interface Effect : ViewSideEffect {

        interface Navigation : Effect {
            object GoToApplication : Navigation
            object GoToSignUp : Navigation
        }
    }

    companion object {
        fun State.isLoginFormValid() = login.isNotEmpty() && password.isNotEmpty()
    }
}