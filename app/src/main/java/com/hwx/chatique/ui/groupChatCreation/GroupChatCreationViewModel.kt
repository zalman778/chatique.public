package com.hwx.chatique.ui.groupChatCreation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupChatCreationViewModel @Inject constructor(
    private val repo: IAppRepo,
    stateHandle: SavedStateHandle,
) : BaseViewModel<GroupChatCreationContract.Event, GroupChatCreationContract.State, GroupChatCreationContract.Effect>() {

    init {
        viewModelScope.launch {
            requestData()
        }
        val isSecret = stateHandle.get<Boolean>(NavigationKeys.Arg.IS_SECRET)
        isSecret?.let {
            setState {
                copy(isSecret = it)
            }
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

    override fun setInitialState() = GroupChatCreationContract.State()

    override fun handleEvents(event: GroupChatCreationContract.Event) = when (event) {
        is GroupChatCreationContract.Event.OnItemToggle -> {
            val wasSelected = viewState.value.selectedList.contains(event.id)
            setState {
                copy(selectedList = selectedList.toMutableList().apply {
                    if (wasSelected) {
                        remove(event.id)
                    } else {
                        add(event.id)
                    }
                })
            }
        }
        is GroupChatCreationContract.Event.OnRetryClick -> {
            viewModelScope.launch {
                requestData()
            }
            Unit
        }
        else -> Unit
    }
}