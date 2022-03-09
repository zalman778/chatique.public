package com.hwx.chatique.ui.signUp

import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.ViewEvent
import com.hwx.chatique.arch.ViewSideEffect
import com.hwx.chatique.arch.ViewState

interface SignUpContract {

    data class State(
        val login: String = "",
        val password: String = "",
        val bio: String = "",
        val email: String = "",
        val isValid: Boolean = false,
        val errorStr: String = "",
        val state: ScreenState = ScreenState.INITIAL,
    ) : ViewState

    interface Event : ViewEvent {
        data class LoginChanged(val value: String) : Event
        data class PasswordChanged(val value: String) : Event
        data class BioChanged(val value: String) : Event
        data class EmailChanged(val value: String) : Event

        object OnSignUpClick : Event
    }

    interface Effect : ViewSideEffect {

        object OnSignUpSuccess : Effect

        interface Navigation : Effect {
            object GoToApplication : Navigation
            object GoToLogin : Navigation
        }
    }

    companion object {
        fun SignUpContract.State.isSignUpFormValid() =
            login.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty()
    }
}