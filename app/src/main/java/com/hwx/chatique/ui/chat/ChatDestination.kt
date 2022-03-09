package com.hwx.chatique.ui.chat

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun ChatDestination(navController: NavHostController) {

    val viewModel: ChatViewModel = hiltViewModel()
    var isRefreshed: Boolean by remember { mutableStateOf(false) }
    isRefreshed = viewModel.init(isRefreshed)
    val state = viewModel.viewState.value
    ChatScreen(
        state = state,
        effectFlow = viewModel.effect,
        onEventSent = { event -> viewModel.setEvent(event) },
    ) { effect ->
        when (effect) {
            is ChatContract.Effect.Navigation.GoToCommunicationRoom ->
                navController.navigate(
                    "${NavigationKeys.Route.COMMUNICATION_ROOM}?${NavigationKeys.Arg.CHAT_ID}=${effect.chatId}&${NavigationKeys.Arg.USER_FROM_ID}=-1&${NavigationKeys.Arg.COMM_ROOM_SCREEN_INITIAL}=${effect.initial}"
                )
        }
    }
}