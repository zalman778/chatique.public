package com.hwx.chatique.ui.friendshipRequests

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun FriendshipRequestsDestination(navController: NavHostController) {

    val viewModel: FriendshipRequestViewModel = hiltViewModel()
    val state = viewModel.viewState.value
    FriendshipRequestsScreen(
        state = state,
        effectFlow = viewModel.effect,
        onEventSent = { event -> viewModel.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
//            if (navigationEffect is FriendsContract.Effect.Navigation.GoToUserDetails) {
//                navController.navigate("${NavigationItem.Friends.route}/${navigationEffect.id}")
//            }
        },
    )
}