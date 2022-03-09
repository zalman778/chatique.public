package com.hwx.chatique.ui.userDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hwx.chatique.R
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.ui.common.LoadingBlock
import com.hwx.chatique.ui.common.RetryBlock

@ExperimentalComposeUiApi
@Composable
fun UserDetailsScreen(
    state: UserDetailsContract.State,
    onEventSent: (event: UserDetailsContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: UserDetailsContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.LOADING -> LoadingBlock()
        ScreenState.RETRY -> RetryBlock {
            onEventSent(UserDetailsContract.Event.OnRetryClick)
        }
        ScreenState.INITIAL, ScreenState.READY -> UserDetailsForm(
            onEventSent,
            state,
            onNavigationRequested,
        )
    }
}

@ExperimentalComposeUiApi
@Composable
private fun UserDetailsForm(
    onEventSent: (event: UserDetailsContract.Event) -> Unit,
    state: UserDetailsContract.State,
    onNavigationRequested: (navigationEffect: UserDetailsContract.Effect.Navigation) -> Unit,
) {
    var isShowingAddToFriendsConfirmation by remember { mutableStateOf(false) }

    Column {
        Text(text = state.username)
        Spacer(modifier = Modifier.height(16.dp))

        //todo - check if exists in friends or friend requests, and not self profile
        OutlinedButton(
            onClick = {
                isShowingAddToFriendsConfirmation = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.add_to_friends))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val targetUserId = state.id
                onNavigationRequested(
                    UserDetailsContract.Effect.Navigation.OnOpenChatClick(
                        targetUserId
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.open_chat))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val targetUserId = state.id
                onNavigationRequested(
                    UserDetailsContract.Effect.Navigation.OnOpenSecretChatClick(
                        targetUserId
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.open_secret_chat))
        }
    }

    if (isShowingAddToFriendsConfirmation) {
        ConfirmationDialog(onEventSent) {
            isShowingAddToFriendsConfirmation = false
        }
    }
}

@Composable
private fun ConfirmationDialog(
    onEventSent: (event: UserDetailsContract.Event) -> Unit,
    dismissDialog: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            dismissDialog()
        },
        title = { Text("Warning!") },
        text = {
            Text(stringResource(id = R.string.add_to_friends_confirmation))
        },
        confirmButton = {
            TextButton(onClick = {
                onEventSent(UserDetailsContract.Event.OnAddToFriendRequested)
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