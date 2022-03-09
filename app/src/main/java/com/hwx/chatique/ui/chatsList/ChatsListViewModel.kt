package com.hwx.chatique.ui.chatsList

import androidx.lifecycle.viewModelScope
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.models.ChatResponseItem
import com.hwx.chatique.network.models.ChatResponseItemType
import com.hwx.chatique.network.models.MessageEvent
import com.hwx.chatique.network.repo.IAppRepo
import com.hwx.chatique.p2p.IE2eController
import com.hwx.chatique.ui.chat.extract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsListViewModel @Inject constructor(
    private val repo: IAppRepo,
    private val e2eController: IE2eController,
    private val streamManager: IStreamManager,
) : BaseViewModel<ChatsListContract.Event, ChatsListContract.State, ChatsListContract.Effect>() {

    fun init(isRefreshed: Boolean): Boolean {
        if (!isRefreshed) {
            viewModelScope.launch {
                requestData()
            }
            listenStreamMessages()
        }
        return true
    }

    private fun listenStreamMessages() {
        viewModelScope.launch {
            streamManager.getStreamInput<MessageEvent>(IStreamManager.StreamId.MESSAGE_EVENT)
                .collect(::onNewMessage)
        }
    }

    private suspend fun requestData() {
        setState {
            copy(state = ScreenState.LOADING, errorStr = "", items = emptyList())
        }
        when (val result = repo.getChats()) {
            is Result.Success -> {
                setState {
                    copy(
                        state = ScreenState.READY,
                        errorStr = "",
                        items = result.value.items
                            .map { it.extract(e2eController) }
                            .toMutableList()
                            .apply {
                                sortByDescending { it.createDate }
                            },
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

    override fun setInitialState() = ChatsListContract.State()

    override fun handleEvents(event: ChatsListContract.Event) = when (event) {
        is ChatsListContract.Event.OnRetryClick -> {
            viewModelScope.launch {
                requestData()
            }
            Unit
        }
        else -> Unit
    }

    private fun onNewMessage(it: MessageEvent) {
        val state = viewState.value
        val chatId = it.dialogId
        val chat = state.items.find { it.id == chatId }
        if (chat == null) {
            requestChatInfoAndAddMessage(chatId, it)
            return
        }
        addMessageToList(chat, it)
    }

    private fun addMessageToList(chat: ChatResponseItem, mesage: MessageEvent) {
        val state = viewState.value
        val isSecret = chat.type == ChatResponseItemType.SECRET
        val message = mesage.extract(isSecret, chat.id, e2eController)
        val targetPositionInList = state.items.indexOf(chat)
        setState {
            copy(items = items.toMutableList().apply {
                this.getOrNull(targetPositionInList)?.let {
                    this[targetPositionInList] = it.copy(lastMessage = message.value)
                }
            })
        }
    }

    private fun requestChatInfoAndAddMessage(chatId: String, message: MessageEvent) {
        viewModelScope.launch {
            when (val result = repo.getChatInfo(chatId)) {
                is Result.Success -> {
                    setState {
                        copy(
                            items = items.toMutableList().apply {
                                add(result.value)
                                sortByDescending { it.createDate }
                            }
                        )
                    }
                    addMessageToList(result.value, message)
                }
            }
        }
    }
}