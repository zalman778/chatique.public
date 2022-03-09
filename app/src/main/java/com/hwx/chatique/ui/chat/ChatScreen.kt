package com.hwx.chatique.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.hwx.chatique.DateFormats
import com.hwx.chatique.R
import com.hwx.chatique.arch.LAUNCH_LISTEN_FOR_EFFECTS
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.models.MessageEvent
import com.hwx.chatique.theme.Shapes
import com.hwx.chatique.ui.common.LoadingBlock
import com.hwx.chatique.ui.common.RetryBlock
import com.hwx.chatique.ui.communicationRoom.CommunicationRoomViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun ChatScreen(
    state: ChatContract.State,
    effectFlow: Flow<ChatContract.Effect>?,
    onEventSent: (event: ChatContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: ChatContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.RETRY -> RetryBlock {
            onEventSent(ChatContract.Event.OnRetryClick)
        }
        ScreenState.INITIAL, ScreenState.READY, ScreenState.LOADING -> ChatForm(
            onEventSent,
            state,
            onNavigationRequested,
            effectFlow,
        )
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun ChatForm(
    onEventSent: (event: ChatContract.Event) -> Unit = {},
    state: ChatContract.State = ChatContract.State(),
    onNavigationRequested: (navigationEffect: ChatContract.Effect.Navigation) -> Unit = {},
    effectFlow: Flow<ChatContract.Effect>?,
) {

    ConstraintLayout(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val (icSend, communicationBlock, inputStrBlock, messagesList) = createRefs()
        var messageStr by remember { mutableStateOf("") }
        val hasRunningCommunication = state.chatInfo.members.any { it.isCommunicating }
        val commBgColor = if (hasRunningCommunication) Color.Green else Color.White
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(commBgColor)
                .constrainAs(communicationBlock) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }
        ) {
            if (hasRunningCommunication) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = "Communication in progress..."
                )
            }
            Image(
                painterResource(R.drawable.ic_call),
                "",
                modifier = Modifier
                    .clickable {
                        val initial = if (hasRunningCommunication) {
                            CommunicationRoomViewModel.ScreenInitial.CONNECT_TO_EXISTING
                        } else
                            CommunicationRoomViewModel.ScreenInitial.OUTGOING
                        onNavigationRequested(
                            ChatContract.Effect.Navigation.GoToCommunicationRoom(
                                state.dialogId, initial,
                            )
                        )
                    }
                    .width(64.dp)
                    .padding(8.dp)
                    .align(Alignment.CenterEnd),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
            )
        }

        Surface(
            elevation = 4.dp,
            shape = Shapes.medium,
            modifier = Modifier
                .height(64.dp)
                .constrainAs(inputStrBlock) {
                    end.linkTo(icSend.start)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                },
        ) {
            TextField(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.White, Shapes.medium),
                value = messageStr,
                onValueChange = {
                    messageStr = it
                },
                singleLine = true,
            )
        }

        val items = state.items
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // Listen for side effects from the VM
        LaunchedEffect(LAUNCH_LISTEN_FOR_EFFECTS) {
            effectFlow?.onEach { effect ->
                when (effect) {
                    is ChatContract.Effect.ScrollToBottom -> {
                        coroutineScope.launch {
                            // Animate scroll to the 1st item
                            listState.animateScrollToItem(index = 0)
                        }
                    }
                }
            }?.collect()
        }

        Image(
            painterResource(R.drawable.ic_baseline_send_24),
            "",
            modifier = Modifier
                .clickable {
                    onEventSent(ChatContract.Event.OnMessageSend(messageStr))
                    messageStr = ""
                    coroutineScope.launch {
                        // Animate scroll to the 10th item
                        listState.animateScrollToItem(index = 0)
                    }
                }
                .width(64.dp)
                .height(64.dp)
                .padding(8.dp)
                .constrainAs(icSend) {
                    end.linkTo(parent.end)
                    start.linkTo(inputStrBlock.end)
                    bottom.linkTo(parent.bottom)
                },
            colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
        )

        if (listState.isScrollInProgress) {
            DisposableEffect(Unit) {
                onDispose {

                    val lastStoreItem = items.lastOrNull()
                    val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()

                    val isLastItem = lastStoreItem?.id == lastItem?.key
                    if (isLastItem) {
                        onEventSent(ChatContract.Event.RequestPage)
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .constrainAs(messagesList) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(communicationBlock.bottom)
                    bottom.linkTo(icSend.top)
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                },
        ) {
            items(items, { listItem: MessageEvent -> listItem.id }) { item ->
                val isSelfMessage = state.currentUserId == item.userFromId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    val cardAlign =
                        if (isSelfMessage) Alignment.CenterEnd else Alignment.CenterStart
                    val bgColor =
                        if (isSelfMessage) colorResource(id = R.color.purple_200) else Color.LightGray
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .width(200.dp)
                            .align(cardAlign),
                        elevation = 4.dp,
                        backgroundColor = bgColor,
                    ) {
                        val dateStr = DateFormats.ddMMHHmm.format(Date(item.createDate * 1000))
                        val prefixWho = if (!isSelfMessage) {
                            "${item.userFrom}:\n"
                        } else ""
                        val messageStr = "$prefixWho ${item.value}\n\n $dateStr"
                        Text(messageStr, color = Color.White)
                    }
                }
            }
        }
    }

    if (state.state == ScreenState.LOADING) {
        LoadingBlock()
    }
}