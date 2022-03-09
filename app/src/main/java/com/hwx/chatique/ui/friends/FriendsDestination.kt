package com.hwx.chatique.ui.friends

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun FriendsDestination(navController: NavHostController) {

    val viewModel: FriendsViewModel = hiltViewModel()
    val state = viewModel.viewState.value
    FriendsScreen(
        state = state,
        onEventSent = { event -> viewModel.setEvent(event) },
    ) { navigationEffect ->
        when (navigationEffect) {
            is FriendsContract.Effect.Navigation.GoToUserDetails ->
                navController.navigate("${NavigationKeys.Route.USER_DETAILS}/${navigationEffect.id}/${navigationEffect.username}")
            is FriendsContract.Effect.Navigation.GoToUserSearch ->
                navController.navigate(NavigationKeys.Route.USER_SEARCH)
        }
    }
}