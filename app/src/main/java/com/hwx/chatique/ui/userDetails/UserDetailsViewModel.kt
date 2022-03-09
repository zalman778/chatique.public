package com.hwx.chatique.ui.userDetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.helpers.tryExtractError
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    private val repo: IAppRepo,
    private val stateHandle: SavedStateHandle,
    private var snackbar: ISnackbarManager,
) : BaseViewModel<UserDetailsContract.Event, UserDetailsContract.State, UserDetailsContract.Effect>() {

    init {
        loadParams()
    }

    private fun loadParams() {
        val userId = stateHandle.get<Long>(NavigationKeys.Arg.USER_IDS)
            ?: throw IllegalStateException("No userId was passed to destination.")
        val username = stateHandle.get<String>(NavigationKeys.Arg.USERNAME)
            ?: throw IllegalStateException("No username was passed to destination.")
        setState {
            copy(id = userId, username = username)
        }
    }

    override fun setInitialState() = UserDetailsContract.State()

    override fun handleEvents(event: UserDetailsContract.Event) {
        when (event) {
            is UserDetailsContract.Event.OnAddToFriendRequested -> requestAddToFriend()
        }
    }

    private fun requestAddToFriend() {
        viewModelScope.launch {
            val userId = viewState.value.id
            when (val result = repo.requestFriendship(userId)) {
                is Result.Success -> {
                    snackbar.showMessage("Friend request has been sent")
                }
                is Result.Fail -> snackbar.tryExtractError(result)
            }
        }
    }
}