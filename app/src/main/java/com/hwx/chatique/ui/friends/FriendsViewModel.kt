package com.hwx.chatique.ui.friends

import androidx.lifecycle.viewModelScope
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.helpers.tryExtractError
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repo: IAppRepo,
    private val snackbar: ISnackbarManager,
) : BaseViewModel<FriendsContract.Event, FriendsContract.State, FriendsContract.Effect>() {

    init {
        viewModelScope.launch {
            requestData()
        }
    }

    private suspend fun requestData() {
        setState {
            copy(state = ScreenState.LOADING, errorStr = "", items = emptyList())
        }
        when (val result = repo.getFriendsList()) {
            is Result.Success -> {
                setState {
                    copy(
                        state = ScreenState.READY,
                        errorStr = "",
                        items = result.value.items
                    )
                }
            }
            is Result.Fail -> {
                setState {
                    copy(state = ScreenState.RETRY, errorStr = errorStr)
                }
            }
        }
    }

    override fun setInitialState() = FriendsContract.State()

    override fun handleEvents(event: FriendsContract.Event) = when (event) {
        is FriendsContract.Event.OnFriendRemovalConfirmed -> {
            viewModelScope.launch {
                when (val result = repo.removeFriend(event.id)) {
                    is Result.Success -> {
                        requestData()
                    }
                    is Result.Fail -> snackbar.tryExtractError(result)
                }
            }
            Unit
        }
        is FriendsContract.Event.OnRetryClick -> {
            viewModelScope.launch {
                requestData()
            }
            Unit
        }
        else -> Unit
    }
}