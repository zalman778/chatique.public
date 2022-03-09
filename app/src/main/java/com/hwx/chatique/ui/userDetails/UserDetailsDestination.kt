package com.hwx.chatique.ui.userDetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys

@ExperimentalComposeUiApi
@Composable
fun UserDetailsDestination(navController: NavHostController) {

    val viewModel: UserDetailsViewModel = hiltViewModel()
    val state = viewModel.viewState.value
    UserDetailsScreen(
        state = state,
        onEventSent = { event -> viewModel.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when (navigationEffect) {
                is UserDetailsContract.Effect.Navigation.OnOpenChatClick -> {
                    navController.navigate("${NavigationKeys.Route.CHAT}?${NavigationKeys.Arg.USER_IDS}=${navigationEffect.id}")
                }
                is UserDetailsContract.Effect.Navigation.OnOpenSecretChatClick -> {
                    navController.navigate("${NavigationKeys.Route.CHAT}?${NavigationKeys.Arg.USER_IDS}=${navigationEffect.id}&${NavigationKeys.Arg.IS_SECRET}=true")
                }
            }
        },
    )
}