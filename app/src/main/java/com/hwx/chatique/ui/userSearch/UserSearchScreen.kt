package com.hwx.chatique.ui.userSearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hwx.chatique.R
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.network.models.SearchUserResponseItem
import com.hwx.chatique.theme.Shapes
import com.hwx.chatique.ui.common.LoadingBlock
import com.hwx.chatique.ui.common.RetryBlock
import com.hwx.chatique.ui.friends.cards.CardTitle

@ExperimentalComposeUiApi
@Composable
fun UserSearchScreen(
    state: UserSearchContract.State,
    onEventSent: (event: UserSearchContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: UserSearchContract.Effect.Navigation) -> Unit,
) {

    when (state.state) {
        ScreenState.LOADING -> LoadingBlock()
        ScreenState.RETRY -> RetryBlock {
            onEventSent(UserSearchContract.Event.OnRetryClick)
        }
        ScreenState.INITIAL, ScreenState.READY -> UserSearchForm(
            onEventSent,
            state,
            onNavigationRequested,
        )
    }
}

@ExperimentalComposeUiApi
@Composable
private fun UserSearchForm(
    onEventSent: (event: UserSearchContract.Event) -> Unit,
    state: UserSearchContract.State,
    onNavigationRequested: (navigationEffect: UserSearchContract.Effect.Navigation) -> Unit,
) {
    val items = state.items
    Column {
        Surface(
            elevation = 4.dp,
            shape = Shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            var searchStr: String by remember { mutableStateOf("") }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, Shapes.medium),
                value = searchStr,
                onValueChange = {
                    searchStr = it
                    onEventSent(UserSearchContract.Event.SearchStrChanged(it))
                },
                label = { Text(text = stringResource(id = R.string.login)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
        ) {
            items(items, { listItem: SearchUserResponseItem -> listItem.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            onNavigationRequested(
                                UserSearchContract.Effect.Navigation.OnUserClick(
                                    item.id,
                                    item.username
                                )
                            )
                        },
                    elevation = 4.dp,

                    ) {
                    CardTitle(cardTitle = item.username)
                }
            }
        }
    }
}