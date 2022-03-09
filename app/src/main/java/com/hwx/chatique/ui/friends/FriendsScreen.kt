package com.hwx.chatique.ui.friends

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.DismissDirection.StartToEnd
import androidx.compose.material.DismissValue.Default
import androidx.compose.material.DismissValue.DismissedToEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hwx.chatique.R
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.models.FriendsListItemResponse
import com.hwx.chatique.ui.common.LoadingBlock
import com.hwx.chatique.ui.common.RetryBlock
import com.hwx.chatique.ui.friends.cards.CardTitle
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun FriendsScreen(
    state: FriendsContract.State,
    onEventSent: (event: FriendsContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: FriendsContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.RETRY -> RetryBlock {
            onEventSent(FriendsContract.Event.OnRetryClick)
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
    onEventSent: (event: FriendsContract.Event) -> Unit,
    state: FriendsContract.State,
    onNavigationRequested: (navigationEffect: FriendsContract.Effect.Navigation) -> Unit,
) {
    var isShowingFriendRemovalConfirmation by remember { mutableStateOf(false) }
    var userId: Long? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val items = state.items
    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painterResource(R.drawable.ic_add),
                "",
                modifier = Modifier
                    .clickable {
                        onNavigationRequested(FriendsContract.Effect.Navigation.GoToUserSearch)
                    }
                    .size(42.dp)
                    .align(Alignment.CenterEnd),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(items, { listItem: FriendsListItemResponse -> listItem.id }) { item ->
                val dismissState = rememberDismissState()
                if (dismissState.isDismissed(StartToEnd)) {
                    userId = item.id.toLong()
                    isShowingFriendRemovalConfirmation = true
                    scope.launch {
                        dismissState.reset()
                    }
                }
                SwipeToDismiss(
                    state = dismissState,
                    modifier = Modifier.padding(vertical = 1.dp),
                    directions = setOf(StartToEnd),
                    dismissThresholds = { FractionalThreshold(0.2f) },
                    background = {
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                DismissedToEnd -> Color.Red
                                else -> Color.Transparent
                            }
                        )
                        val alignment = Alignment.CenterStart
                        val icon = Icons.Default.Delete

                        val scale by animateFloatAsState(
                            if (dismissState.targetValue == Default) 0.75f else 1f
                        )

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = alignment
                        ) {
                            Icon(
                                icon,
                                contentDescription = "Localized description",
                                modifier = Modifier.scale(scale)
                            )
                        }
                    },
                    dismissContent = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    onNavigationRequested(
                                        FriendsContract.Effect.Navigation.GoToUserDetails(
                                            item.id, item.username
                                        )
                                    )
                                },
                            elevation = animateDpAsState(
                                if (dismissState.dismissDirection != null) 4.dp else 0.dp
                            ).value,

                            ) {
                            CardTitle(cardTitle = item.username)
                        }
                    }
                )
            }
        }
    }

    if (isShowingFriendRemovalConfirmation && userId != null) {
        ConfirmationDialog(onEventSent, userId!!) {
            isShowingFriendRemovalConfirmation = false
        }
    }

    if (state.state == ScreenState.LOADING) {
        LoadingBlock()
    }

}

@Composable
private fun ConfirmationDialog(
    onEventSent: (event: FriendsContract.Event) -> Unit,
    userId: Long,
    dismissDialog: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            dismissDialog()
        },
        title = { Text("Warning!") },
        text = {
            Text(stringResource(id = R.string.remove_friend_confirmation))
        },
        confirmButton = {
            TextButton(onClick = {
                onEventSent(FriendsContract.Event.OnFriendRemovalConfirmed(userId))
                dismissDialog()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dismissDialog()
            }) {
                Text("Cancel")
            }
        }
    )
}