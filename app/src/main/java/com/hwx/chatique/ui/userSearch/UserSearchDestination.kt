package com.hwx.chatique.ui.userSearch

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys

@ExperimentalComposeUiApi
@Composable
fun UserSearchDestination(navController: NavHostController) {

    val viewModel: UserSearchViewModel = hiltViewModel()
    val state = viewModel.viewState.value
    UserSearchScreen(
        state = state,
        onEventSent = { event -> viewModel.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when (navigationEffect) {
                is UserSearchContract.Effect.Navigation.OnUserClick -> {
                    navController
                        .navigate("${NavigationKeys.Route.USER_DETAILS}/${navigationEffect.id}/${navigationEffect.username}")
                }
            }
        },
    )
}