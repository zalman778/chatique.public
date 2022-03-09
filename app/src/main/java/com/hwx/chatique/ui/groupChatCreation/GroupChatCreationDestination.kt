package com.hwx.chatique.ui.groupChatCreation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun GroupChatCreationDestination(navController: NavHostController) {

    val viewModel: GroupChatCreationViewModel = hiltViewModel()
    val state = viewModel.viewState.value
    GroupChatCreationScreen(
        state = state,
        onEventSent = { event -> viewModel.setEvent(event) },
    ) { navigationEffect ->
        when (navigationEffect) {
            is GroupChatCreationContract.Effect.Navigation.GoToChat -> {
                val userIds = navigationEffect.targetUserIds.joinToString(";")
                navController.navigate("${NavigationKeys.Route.CHAT}?${NavigationKeys.Arg.IS_SECRET}=${navigationEffect.isSecret}&${NavigationKeys.Arg.USER_IDS}=${userIds}") {
                    navController.graph.startDestinationRoute?.let { route ->
                        popUpTo(route) {
                            saveState = true
                        }
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}