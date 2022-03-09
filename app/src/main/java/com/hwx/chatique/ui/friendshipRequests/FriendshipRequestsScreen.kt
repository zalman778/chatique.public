package com.hwx.chatique.ui.friendshipRequests

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
import androidx.compose.material.DismissDirection.EndToStart
import androidx.compose.material.DismissDirection.StartToEnd
import androidx.compose.material.DismissValue.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
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
import com.hwx.chatique.network.models.FriendshipRequestType
import com.hwx.chatique.network.models.FriendshipRequestsItem
import com.hwx.chatique.ui.common.RetryBlock
import com.hwx.chatique.ui.friends.cards.CardTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun FriendshipRequestsScreen(
    state: FriendshipRequestsContract.State,
    effectFlow: Flow<FriendshipRequestsContract.Effect>?,
    onEventSent: (event: FriendshipRequestsContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: FriendshipRequestsContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.LOADING -> {
            AlertDialog(
                onDismissRequest = {

                },
                title = {},
                text = {
                    CircularProgressIndicator()
                },
                buttons = {},
            )
        }
        ScreenState.RETRY -> RetryBlock {
            onEventSent(FriendshipRequestsContract.Event.OnRetryClick)
        }
        ScreenState.INITIAL, ScreenState.READY -> FriendshipRequestsForm(
            onEventSent,
            state,
            onNavigationRequested,
        )
    }
}

enum class ActionType(val messageResId: Int) {
    ACCEPT(R.string.alert_accept_fr_req),
    DECLINE(R.string.decline_accept_fr_req),
}

//https://stackoverflow.com/a/68314565

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
private fun FriendshipRequestsForm(
    onEventSent: (event: FriendshipRequestsContract.Event) -> Unit,
    state: FriendshipRequestsContract.State,
    onNavigationRequested: (navigationEffect: FriendshipRequestsContract.Effect.Navigation) -> Unit,
) {
    val items = state.items.filter { it.type == state.type }
    var alertType: ActionType? by remember { mutableStateOf(null) }
    var alertItemId: String by remember { mutableStateOf("") }

    val updateDialog = { type: ActionType?, id: String ->
        alertType = type
        alertItemId = id
    }

    if (alertType != null && alertItemId != "") {
        ConfirmationDialog(updateDialog, alertType, alertItemId, onEventSent)
    }

    val scope = rememberCoroutineScope()

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = state.type.captionResId),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterStart),
            )
            Image(
                painterResource(R.drawable.ic_baseline_360_24),
                "",
                modifier = Modifier
                    .clickable {
                        onEventSent(FriendshipRequestsContract.Event.OnSwitchTypeClick)
                    }
                    .size(42.dp)
                    .align(Alignment.CenterEnd),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(items, { listItem: FriendshipRequestsItem -> listItem.id }) { item ->
                val dismissState = rememberDismissState()
                if (dismissState.isDismissed(StartToEnd)) {
                    alertType = ActionType.DECLINE
                    alertItemId = item.id
                    scope.launch {
                        dismissState.reset()
                    }
                }
                if (dismissState.isDismissed(EndToStart)) {
                    alertType = ActionType.ACCEPT
                    alertItemId = item.id
                    scope.launch {
                        dismissState.reset()
                    }
                }

                val swipeDirections = if (state.type == FriendshipRequestType.INPUT) {
                    setOf(StartToEnd, EndToStart)
                } else {
                    setOf(StartToEnd)
                }

                SwipeToDismiss(
                    state = dismissState,
                    modifier = Modifier.padding(vertical = 1.dp),
                    directions = swipeDirections,
                    dismissThresholds = { _ -> FractionalThreshold(0.25f) },
                    background = {
                        val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                DismissedToEnd -> Color.Red
                                DismissedToStart -> Color.Green
                                else -> Color.Transparent
                            }
                        )
                        val alignment = when (direction) {
                            StartToEnd -> Alignment.CenterStart
                            EndToStart -> Alignment.CenterEnd
                        }
                        val icon = when (direction) {
                            StartToEnd -> Icons.Default.Delete
                            EndToStart -> Icons.Default.Done
                        }

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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
}

@Composable
private fun ConfirmationDialog(
    updateDialog: (ActionType?, String) -> Unit,
    alertType: ActionType?,
    alertItemId: String,
    onEventSent: (event: FriendshipRequestsContract.Event) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            updateDialog(null, "")
        },
        title = { Text("Warning!") },
        text = {
            Text(
                alertType?.messageResId?.let {
                    stringResource(id = it)
                } ?: ""
            )
        },
        confirmButton = {
            TextButton(onClick = {
                when (alertType) {
                    ActionType.ACCEPT -> FriendshipRequestsContract.Event.OnItemAccept(alertItemId)
                    ActionType.DECLINE -> FriendshipRequestsContract.Event.OnItemDecline(alertItemId)
                    else -> null
                }?.let {
                    onEventSent(it)
                }
                updateDialog(null, "")
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                updateDialog(null, "")
            }) {
                Text("Cancel")
            }
        }
    )
}