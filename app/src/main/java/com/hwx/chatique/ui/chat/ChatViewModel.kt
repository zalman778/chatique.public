package com.hwx.chatique.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.arch.BaseViewModel
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.arch.extensions.isActiveJob
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.helpers.tryExtractError
import com.hwx.chatique.network.IStreamManager
import com.hwx.chatique.network.Result
import com.hwx.chatique.network.models.*
import com.hwx.chatique.network.repo.IAppRepo
import com.hwx.chatique.p2p.IE2eController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: IAppRepo,
    private val snackbar: ISnackbarManager,
    private val streamManager: IStreamManager,
    private val profileHolder: IProfileHolder,
    private val stateHandle: SavedStateHandle,
    private val e2eController: IE2eController,
) : BaseViewModel<ChatContract.Event, ChatContract.State, ChatContract.Effect>() {

    private companion object {
        const val PAGE_LIMIT = 10L
    }

    private var loadingJob: Job? = null
    private var isSecretChat = false
    private var dialogId = ""

    fun init(isRefreshed: Boolean): Boolean {
        if (!isRefreshed) {
            initInternal()
        }
        return true
    }

    private fun initInternal() {
        readInputParams()
        val currentUserId = profileHolder.userId
        setState {
            copy(currentUserId = currentUserId)
        }
        if (dialogId.isNotEmpty()) {
            loadDialog()
        } else {
            createDialog()
        }

        listenStreamMessages()
    }

    private fun readInputParams() {
        isSecretChat = stateHandle.get<Boolean>(NavigationKeys.Arg.IS_SECRET) ?: false
        dialogId = stateHandle.get<String>(NavigationKeys.Arg.CHAT_ID) ?: ""
        setState {
            copy(dialogId = this@ChatViewModel.dialogId)
        }
    }

    private fun listenStreamMessages() {
        viewModelScope.launch {
            streamManager.getStreamInput<MessageEvent>(IStreamManager.StreamId.MESSAGE_EVENT)
                .collect(::onNewMessage)
        }
    }

    private fun loadDialog() {
        requestChatInfo()
        requestPageInternal(0L)
    }

    private fun createDialog() {
        val currentUserId = profileHolder.userId
        val chatType = if (isSecretChat) ChatResponseItemType.SECRET else ChatResponseItemType.OPEN
        val userIds = stateHandle
            .get<String>(NavigationKeys.Arg.USER_IDS)
            ?.split(";")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
        val targetMemberIds = userIds.toMutableList().apply { add(currentUserId) }
        val request = CreateChatRequest(chatType, targetMemberIds)
        viewModelScope.launch {
            when (val result = repo.createChat(request)) {
                is Result.Success -> {
                    val newChatId = result.value.id
                    dialogId = newChatId
                    setState {
                        copy(dialogId = this@ChatViewModel.dialogId)
                    }
                    if (isSecretChat) {
                        e2eController.startClientFlow(
                            newChatId,
                            result.value.membersOnline,
                            userIds
                        )
                        setState {
                            copy(
                                type = chatType,
                            )
                        }
                    }
                }
                is Result.Fail -> snackbar.tryExtractError(result)
            }
        }
    }

    private fun requestChatInfo() {
        viewModelScope.launch {
            when (val result = repo.getChatInfo(dialogId)) {
                is Result.Success -> {
                    setState {
                        copy(chatInfo = result.value)
                    }
                    onChatInfoLoaded()
                }
            }
        }
    }

    private fun onChatInfoLoaded() {
        val chatInfo = viewState.value.chatInfo
        val isNeedKeySharing = chatInfo.groupType == ChatResponseItemGroupType.GROUP
        val hasKey = e2eController.getRemoteKey(chatInfo.id, 0L) != null
        if (isNeedKeySharing && !hasKey) {
            setState {
                copy(state = ScreenState.LOADING)
            }
            e2eController.requestKeySharing(
                chatInfo.id,
                chatInfo.members.map { it.userId }
            )

            viewModelScope.launch {
                e2eController.availableKeys.filter { it.contains(chatInfo.id) }.firstOrNull()
                val hasKeyNew = e2eController.getRemoteKey(chatInfo.id, 0L) != null
                onSecretChatKeyObtained()
                if (hasKeyNew) {
                    setState {
                        copy(state = ScreenState.READY)
                    }
                }
            }
        }
    }

    private fun onSecretChatKeyObtained() {
        setState {
            copy(items = viewState.value.items.map { it.extract(true, dialogId, e2eController) })
        }
    }

    override fun setInitialState() = ChatContract.State()

    override fun handleEvents(event: ChatContract.Event) = when (event) {
        is ChatContract.Event.OnMessageSend -> sendMessage(event.value)
        is ChatContract.Event.OnRetryClick -> {
//            viewModelScope.launch {
//                requestData()
//            }
//            Unit
        }
        is ChatContract.Event.RequestPage -> onPageLoad()
        else -> Unit
    }

    private fun onPageLoad() {
        if (loadingJob.isActiveJob()) return
        val state = viewState.value
        if (state.currentPage + 1 >= state.totalPages) return
        val page = state.currentPage + 1
        requestPageInternal(page)
    }

    private fun requestPageInternal(
        page: Long,
    ) {
        val state = viewState.value
        setState {
            copy(state = ScreenState.LOADING)
        }
        val request = ChatHistoryRequest(
            dialogId,
            page,
            PAGE_LIMIT,
        )
        loadingJob = viewModelScope.launch {
            val result = repo.getChatHistory(request)
            when (result) {
                is Result.Success -> {
                    val newItemsList = state.items.toMutableList().apply {
                        result.value.items
                            .map { it.extract(isSecretChat, dialogId, e2eController) }
                            .forEach {
                                if (!contains(it)) {
                                    add(it)
                                }
                            }
                    }
                    setState {
                        copy(
                            items = newItemsList,
                            currentPage = request.page,
                            totalPages = result.value.totalPages,
                        )
                    }
                }
            }
            setState {
                copy(state = ScreenState.READY)
            }
        }
    }

    private fun sendMessage(value: String) {
        val event = MessageEvent(
            UUID.randomUUID().toString(),
            dialogId,
            profileHolder.userId,
            value,
            Date().time / 1000,
            e2eController.getKeyVersion(dialogId),
            profileHolder.username,
        )
        val preparedEvent = event.prepareToSend(isSecretChat, dialogId, e2eController)
        streamManager.sendMessageToStream(IStreamManager.StreamId.MESSAGE_EVENT, preparedEvent)

        setState {
            val newList = viewState.value.items.toMutableList().apply {
                add(0, event)
            }
            copy(items = newList)
        }
    }

    private fun onNewMessage(it: MessageEvent) {
        setState {
            copy(items = items.toMutableList().apply {
                add(0, it.extract(isSecretChat, this@ChatViewModel.dialogId, e2eController))
            })
        }
        setEffect {
            //todo - handle this
            ChatContract.Effect.ScrollToBottom

        }
    }
}