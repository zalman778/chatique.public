package com.hwx.chatique.ui.chatsList

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwx.chatique.R
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.network.models.ChatResponseItem
import com.hwx.chatique.network.models.ChatResponseItemGroupType
import com.hwx.chatique.network.models.ChatResponseItemType
import com.hwx.chatique.p2p.IE2eController
import com.hwx.chatique.ui.common.LoadingBlock
import com.hwx.chatique.ui.common.RetryBlock

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun ChatsListScreen(
    state: ChatsListContract.State,
    onEventSent: (event: ChatsListContract.Event) -> Unit,
    profileHolder: IProfileHolder,
    dhCoordinator: IE2eController,
    onNavigationRequested: (navigationEffect: ChatsListContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.RETRY -> RetryBlock {
            onEventSent(ChatsListContract.Event.OnRetryClick)
        }
        ScreenState.INITIAL, ScreenState.READY, ScreenState.LOADING -> ChatsForm(
            onEventSent,
            state,
            onNavigationRequested,
            profileHolder,
            dhCoordinator,
        )
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun ChatsForm(
    onEventSent: (event: ChatsListContract.Event) -> Unit,
    state: ChatsListContract.State,
    onNavigationRequested: (navigationEffect: ChatsListContract.Effect.Navigation) -> Unit,
    profileHolder: IProfileHolder,
    dhCoordinator: IE2eController,
) {
    val items = state.items
    Column {
        var isAddMenuVisible by remember { mutableStateOf(false) }
        DropdownMenu(
            expanded = isAddMenuVisible,
            onDismissRequest = { isAddMenuVisible = false }
        ) {
            DropdownMenuItem(onClick = {
                onNavigationRequested(ChatsListContract.Effect.Navigation.GoToChatCreation())
            }) {
                Text("Create group chat")
            }
            Divider()
            DropdownMenuItem(onClick = {
                onNavigationRequested(ChatsListContract.Effect.Navigation.GoToChatCreation(isSecret = true))
            }) {
                Text("Create secret group chat")
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painterResource(R.drawable.ic_add),
                "",
                modifier = Modifier
                    .clickable {
                        isAddMenuVisible = true

                    }
                    .size(42.dp)
                    .align(Alignment.CenterEnd),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(items, { listItem: ChatResponseItem -> listItem.id }) { item ->
                Column(
                    modifier = Modifier
                        .clickable {
                            val currentUserId = profileHolder.userId
                            val isSecret = item.type == ChatResponseItemType.SECRET
                            val targetUserId =
                                if (item.groupType == ChatResponseItemGroupType.DUAL) {
                                    item.members.find { it.userId != currentUserId }?.userId ?: -1
                                } else {
                                    -1
                                }

                            onNavigationRequested(
                                ChatsListContract.Effect.Navigation.GoToChat(
                                    item.id,
                                    isSecret,
                                    targetUserId,
                                )
                            )
                        }
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val textColor =
                        if (item.type == ChatResponseItemType.SECRET) Color.Green else Color.Black
                    Text(
                        text = item.caption + " (" + item.members.joinToString { it.username } + ")",
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Start,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.lastMessage,
                        fontSize = 16.sp,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Start,
                    )

                    Divider(color = Color.DarkGray, thickness = 1.dp)
                }
            }
        }
    }
    if (state.state == ScreenState.LOADING) {
        LoadingBlock()
    }
}