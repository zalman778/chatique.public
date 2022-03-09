package com.hwx.chatique.ui.groupChatCreation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hwx.chatique.R
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.models.FriendsListItemResponse
import com.hwx.chatique.ui.common.LoadingBlock
import com.hwx.chatique.ui.common.RetryBlock
import com.hwx.chatique.ui.friends.cards.CardTitle

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun GroupChatCreationScreen(
    state: GroupChatCreationContract.State,
    onEventSent: (event: GroupChatCreationContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: GroupChatCreationContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.RETRY -> RetryBlock {
            onEventSent(GroupChatCreationContract.Event.OnRetryClick)
        }
        ScreenState.INITIAL, ScreenState.READY, ScreenState.LOADING -> FriendsForm(
            onEventSent,
            state,
            onNavigationRequested,
        )
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun FriendsForm(
    onEventSent: (event: GroupChatCreationContract.Event) -> Unit,
    state: GroupChatCreationContract.State,
    onNavigationRequested: (navigationEffect: GroupChatCreationContract.Effect.Navigation) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val items = state.items
    Column {
        val isAcceptVisible = state.selectedList.isNotEmpty()
        if (isAcceptVisible) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painterResource(R.drawable.ic_done),
                    "",
                    modifier = Modifier
                        .clickable {
                            onNavigationRequested(
                                GroupChatCreationContract.Effect.Navigation.GoToChat(
                                    state.isSecret,
                                    state.selectedList,
                                )
                            )
                        }
                        .size(42.dp)
                        .align(Alignment.CenterEnd),
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(items, { listItem: FriendsListItemResponse -> listItem.id }) { item ->
                val isSelected = state.selectedList.contains(item.id)
                val bgColor = if (isSelected) Color.Green else Color.White
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            onEventSent(GroupChatCreationContract.Event.OnItemToggle(item.id))
                        },
                    elevation = 4.dp,
                    backgroundColor = bgColor,
                ) {
                    CardTitle(cardTitle = item.username)
                }
            }
        }
    }

    if (state.state == ScreenState.LOADING) {
        LoadingBlock()
    }

}