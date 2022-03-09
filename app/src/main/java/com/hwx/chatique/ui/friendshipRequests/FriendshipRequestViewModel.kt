package com.hwx.chatique.ui.friendshipRequests

import androidx.lifecycle.viewModelScope
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.helpers.tryExtractError
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.models.FriendshipRequestType
import com.hwx.chatique.network.models.HandleFriendshipRequestRequest
import com.hwx.chatique.network.models.HandleFriendshipRequestType
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendshipRequestViewModel @Inject constructor(
    private val repo: IAppRepo,
    private val snackbar: ISnackbarManager,
) : BaseViewModel<FriendshipRequestsContract.Event, FriendshipRequestsContract.State, FriendshipRequestsContract.Effect>() {

    init {
        viewModelScope.launch {
            requestData()
        }
    }

    private suspend fun requestData() {
        setState {
            copy(state = ScreenState.LOADING, errorStr = "")
        }
        when (val result = repo.getFriendshipRequests()) {
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
                //todo - show retry state!
                setState {
                    copy(state = ScreenState.RETRY, errorStr = errorStr)
                }
            }
        }
    }

    override fun setInitialState() = FriendshipRequestsContract.State()

    override fun handleEvents(event: FriendshipRequestsContract.Event) = when (event) {
        is FriendshipRequestsContract.Event.OnItemDecline ->
            handleRequest(event.id, HandleFriendshipRequestType.DECLINE)
        is FriendshipRequestsContract.Event.OnItemAccept ->
            handleRequest(event.id, HandleFriendshipRequestType.ACCEPT)
        is FriendshipRequestsContract.Event.OnSwitchTypeClick -> {
            val newType = if (viewState.value.type == FriendshipRequestType.INPUT) {
                FriendshipRequestType.OUTPUT
            } else {
                FriendshipRequestType.INPUT
            }
            setState {
                copy(type = newType)
            }
        }
        FriendshipRequestsContract.Event.OnRetryClick -> {
            viewModelScope.launch {
                requestData()
            }
            Unit
        }
        else -> Unit
    }

    private fun handleRequest(id: String, reactionType: HandleFriendshipRequestType) {
        viewModelScope.launch {
            val request =
                HandleFriendshipRequestRequest(id, reactionType)
            when (val result = repo.handleFriendshipRequest(request)) {
                is Result.Success -> {
                    requestData()
                }
                is Result.Fail -> snackbar.tryExtractError(result)
            }
        }
    }
}