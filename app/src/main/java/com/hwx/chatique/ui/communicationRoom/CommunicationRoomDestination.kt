package com.hwx.chatique.ui.communicationRoom

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.flow.video.IVideoController

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun CommunicationRoomDestination(
    navController: NavHostController,
    videoController: IVideoController,
) {

    val viewModel: CommunicationRoomViewModel = hiltViewModel()
    var isRefreshed: Boolean by remember { mutableStateOf(false) }
    isRefreshed = viewModel.init(isRefreshed)
    val state = viewModel.viewState.value
    CommunicationRoomScreen(
        state = state,
        onEventSent = { event -> viewModel.setEvent(event) },
        videoController,
    ) { navigationEffect ->
//        when (navigationEffect) {
//            is Co.Effect.Navigation.GoToUserDetails ->
//                navController.navigate("${NavigationKeys.Route.USER_DETAILS}/${navigationEffect.id}/${navigationEffect.username}")
//            is Co.Effect.Navigation.GoToUserSearch ->
//                navController.navigate(NavigationKeys.Route.USER_SEARCH)
//        }
    }
}