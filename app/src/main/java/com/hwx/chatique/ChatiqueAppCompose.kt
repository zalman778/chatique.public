package com.hwx.chatique

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hwx.chatique.flow.IAuthInteractor
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.flow.video.IVideoController
import com.hwx.chatique.helpers.INavigationHolder
import com.hwx.chatique.helpers.ISnackbarManager
import com.hwx.chatique.p2p.IE2eController
import com.hwx.chatique.ui.base.BottomNavigationBar
import com.hwx.chatique.ui.base.NavigationItem
import com.hwx.chatique.ui.chat.ChatDestination
import com.hwx.chatique.ui.chatsList.ChatsListDestination
import com.hwx.chatique.ui.communicationRoom.CommunicationRoomDestination
import com.hwx.chatique.ui.enterPin.EnterPinScreen
import com.hwx.chatique.ui.friends.FriendsDestination
import com.hwx.chatique.ui.friendshipRequests.FriendshipRequestsDestination
import com.hwx.chatique.ui.groupChatCreation.GroupChatCreationDestination
import com.hwx.chatique.ui.login.LoginDestination
import com.hwx.chatique.ui.main.HomeScreen
import com.hwx.chatique.ui.main.ProfileScreen
import com.hwx.chatique.ui.signUp.SignUpDestination
import com.hwx.chatique.ui.userDetails.UserDetailsDestination
import com.hwx.chatique.ui.userSearch.UserSearchDestination

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun ChatiqueApp(
    authInteractor: IAuthInteractor,
    snackbarManager: ISnackbarManager,
    profileHolder: IProfileHolder,
    dhCoordinator: IE2eController,
    navigationHolder: INavigationHolder,
    videoController: IVideoController,
) {
    var isBottomNavigationVisible: Boolean by remember { mutableStateOf(true) }
    val authState = authInteractor.state.collectAsState()
    val navController = rememberNavController()
    navigationHolder.init(navController)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    snackbarManager.host = snackbarHostState
    snackbarManager.scope = coroutineScope

    var startDestination = when (authState.value) {
        IAuthInteractor.State.NO_AUTH -> NavigationKeys.Route.LOGIN
        IAuthInteractor.State.SET_PIN,
        IAuthInteractor.State.ENTER_PIN,
        -> NavigationKeys.Route.SET_PIN
        IAuthInteractor.State.AUTHENTICATED -> NavigationItem.Home.route
    }

    Scaffold(
        bottomBar = {
            if (authState.value == IAuthInteractor.State.AUTHENTICATED && isBottomNavigationVisible) {
                BottomNavigationBar(navController)
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) {
        NavHost(navController, startDestination = startDestination) {
            composable(NavigationKeys.Route.LOGIN) {
                LoginDestination(navController)
            }
            composable(NavigationKeys.Route.SIGN_UP) {
                SignUpDestination(navController)
            }
            composable(NavigationKeys.Route.SET_PIN) {
                val isSetting = authState.value == IAuthInteractor.State.SET_PIN
                EnterPinScreen(authInteractor, isSetting)
            }
            composable(NavigationKeys.Route.USER_SEARCH) {
                isBottomNavigationVisible = false
                UserSearchDestination(navController)
            }
            composable(
                route = NavigationKeys.Route.USER_DETAILS_ROUTE,
                arguments = listOf(
                    navArgument(NavigationKeys.Arg.USER_IDS) {
                        type = NavType.LongType
                    },
                    navArgument(NavigationKeys.Arg.USERNAME) {
                        type = NavType.StringType
                    },
                )
            ) {
                isBottomNavigationVisible = false
                UserDetailsDestination(navController)
            }
            composable(
                route = NavigationKeys.Route.CHAT_ROUTE,
                arguments = listOf(
                    navArgument(NavigationKeys.Arg.USER_IDS) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NavigationKeys.Arg.CHAT_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                    navArgument(NavigationKeys.Arg.IS_SECRET) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument(NavigationKeys.Arg.IS_VOICE_CALL_ACCEPTED) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument(NavigationKeys.Arg.USER_FROM_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                )
            ) {
                isBottomNavigationVisible = false
                ChatDestination(navController)
            }
            composable(
                route = NavigationKeys.Route.GROUP_CHAT_CREATION_ROUTE,
                arguments = listOf(
                    navArgument(NavigationKeys.Arg.IS_SECRET) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                )
            ) {
                isBottomNavigationVisible = false
                GroupChatCreationDestination(navController)
            }
            composable(NavigationItem.Friends.route) {
                isBottomNavigationVisible = true
                FriendsDestination(navController)
            }
            composable(NavigationItem.Chats.route) {
                isBottomNavigationVisible = true
                ChatsListDestination(navController, profileHolder, dhCoordinator)
            }
            composable(NavigationItem.FriendshipRequests.route) {
                FriendshipRequestsDestination(navController)
            }
            composable(NavigationItem.Home.route) {
                HomeScreen()
            }
            composable(NavigationItem.Profile.route) {
                ProfileScreen(authInteractor, navController, dhCoordinator, profileHolder)
            }
            composable(
                route = NavigationKeys.Route.COMMUNICATION_ROOM_ROUTE,
                arguments = listOf(
                    navArgument(NavigationKeys.Arg.CHAT_ID) {
                        type = NavType.StringType
                    },
                    navArgument(NavigationKeys.Arg.USER_FROM_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(NavigationKeys.Arg.COMM_ROOM_SCREEN_INITIAL) {
                        type = NavType.StringType
                    },
                )
            ) {
                isBottomNavigationVisible = false
                CommunicationRoomDestination(navController, videoController)
            }
        }
    }
}