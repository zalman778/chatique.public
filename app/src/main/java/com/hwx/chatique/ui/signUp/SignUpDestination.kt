package com.hwx.chatique.ui.signUp

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.ui.base.NavigationItem

@ExperimentalComposeUiApi
@Composable
fun SignUpDestination(navController: NavHostController) {

    val viewModel: SignUpViewModel = hiltViewModel()
    val state = viewModel.viewState.value
    SignUpScreen(
        state = state,
        effectFlow = viewModel.effect,
        onEventSent = { event -> viewModel.setEvent(event) },
        onNavigationRequested = { navigationEffect ->
            when (navigationEffect) {
                is SignUpContract.Effect.Navigation.GoToApplication -> {
                    navController.navigate(NavigationItem.Home.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
                is SignUpContract.Effect.Navigation.GoToLogin -> {
                    navController.navigate(NavigationKeys.Route.LOGIN)
                }
            }
        },
    )
}