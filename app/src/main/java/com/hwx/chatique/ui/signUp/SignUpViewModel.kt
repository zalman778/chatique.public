package com.hwx.chatique.ui.signUp

import androidx.lifecycle.viewModelScope
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.models.SignUpRequest
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repo: IAppRepo,
) : BaseViewModel<SignUpContract.Event, SignUpContract.State, SignUpContract.Effect>() {

    override fun setInitialState() = SignUpContract.State()

    override fun handleEvents(event: SignUpContract.Event) {
        when (event) {
            is SignUpContract.Event.LoginChanged -> {
                setState {
                    copy(login = event.value)
                }
            }
            is SignUpContract.Event.PasswordChanged -> {
                setState {
                    copy(password = event.value)
                }
            }
            is SignUpContract.Event.BioChanged -> {
                setState {
                    copy(bio = event.value)
                }
            }
            is SignUpContract.Event.EmailChanged -> {
                setState {
                    copy(email = event.value)
                }
            }
            is SignUpContract.Event.OnSignUpClick -> requestSignUp()
        }
    }

    private fun requestSignUp() {
        viewModelScope.launch {
            setState {
                copy(state = ScreenState.LOADING, errorStr = "")
            }
            val state = viewState.value
            val request = SignUpRequest(state.login, state.password, state.bio, state.email)
            when (repo.signUp(request)) {
                is Result.Success -> {
                    setState {
                        copy(state = ScreenState.READY, errorStr = "")
                    }
                    setEffect { SignUpContract.Effect.OnSignUpSuccess }
                }
                else -> {
                    setState {
                        copy(state = ScreenState.RETRY, errorStr = errorStr)
                    }
                }
            }
        }
    }
}