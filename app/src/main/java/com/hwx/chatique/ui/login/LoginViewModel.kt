package com.hwx.chatique.ui.login

import androidx.lifecycle.viewModelScope
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.flow.IAuthInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authInteractor: IAuthInteractor,
) : BaseViewModel<LoginContract.Event, LoginContract.State, LoginContract.Effect>() {

    override fun setInitialState() = LoginContract.State()

    override fun handleEvents(event: LoginContract.Event) {
        when (event) {
            is LoginContract.Event.LoginChanged -> {
                setState {
                    copy(login = event.value)
                }
            }
            is LoginContract.Event.PasswordChanged -> {
                setState {
                    copy(password = event.value)
                }
            }
            is LoginContract.Event.OnLoginClick -> requestLogin()
        }
    }

    private fun requestLogin() {
        viewModelScope.launch {
            setState {
                copy(state = ScreenState.LOADING, errorStr = "")
            }
            val state = viewState.value
            authInteractor.authWithLoginPassword(state.login, state.password)
            setState {
                copy(state = ScreenState.INITIAL, errorStr = "")
            }
        }
    }
}