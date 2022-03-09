package com.hwx.chatique.ui.userSearch

import androidx.lifecycle.viewModelScope
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.repo.IAppRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val repo: IAppRepo,
) : BaseViewModel<UserSearchContract.Event, UserSearchContract.State, UserSearchContract.Effect>() {

    private companion object {
        const val SEARCH_DELAY = 500L
    }

    private var searchDelayJob: Job? = null

    override fun setInitialState() = UserSearchContract.State()

    override fun handleEvents(event: UserSearchContract.Event) {
        when (event) {
            is UserSearchContract.Event.SearchStrChanged -> {
                if (event.value.isEmpty()) return
                searchDelayJob?.cancel()
                searchDelayJob = viewModelScope.launch {
                    delay(SEARCH_DELAY)
                    requestSearch(event.value)
                }
            }
        }
    }

    private fun requestSearch(searchStr: String) {
        viewModelScope.launch {
            setState {
                copy(state = ScreenState.LOADING, errorStr = "")
            }
            when (val result = repo.searchUser(searchStr)) {
                is Result.Success -> {
                    setState {
                        copy(
                            state = ScreenState.READY,
                            errorStr = "",
                            items = result.value.items
                        )
                    }
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