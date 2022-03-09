package com.hwx.chatique.ui.chatsList

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.p2p.IE2eController

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun ChatsListDestination(
    navController: NavHostController,
    profileHolder: IProfileHolder,
    dhCoordinator: IE2eController
) {

    val viewModel: ChatsListViewModel = hiltViewModel()
    var isRefreshed: Boolean by remember { mutableStateOf(false) }
    isRefreshed = viewModel.init(isRefreshed)
    val state = viewModel.viewState.value
    ChatsListScreen(
        state = state,
        onEventSent = { event -> viewModel.setEvent(event) },
        profileHolder,
        dhCoordinator,
    ) { navigationEffect ->
        when (navigationEffect) {
            is ChatsListContract.Effect.Navigation.GoToChat ->
                navController.navigate("${NavigationKeys.Route.CHAT}?${NavigationKeys.Arg.CHAT_ID}=${navigationEffect.id}&${NavigationKeys.Arg.IS_SECRET}=${navigationEffect.isSecret}&${NavigationKeys.Arg.USER_IDS}=${navigationEffect.targetUserId}")
            is ChatsListContract.Effect.Navigation.GoToChatCreation ->
                navController.navigate("${NavigationKeys.Route.GROUP_CHAT_CREATION}?${NavigationKeys.Arg.IS_SECRET}=${navigationEffect.isSecret}")
        }
    }
}