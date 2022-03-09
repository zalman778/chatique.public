package com.hwx.chatique.ui.base

import com.hwx.chatique.R

sealed class NavigationItem(var route: String, var icon: Int, var title: String) {
    object Friends : NavigationItem("friends", R.drawable.ic_users_circle_24, "Friends")

    object FriendshipRequests :
        NavigationItem("friendshipRequests", R.drawable.ic_user_add, "F. Reqs")

    object Home : NavigationItem("home", R.drawable.ic_eye, "Home")
    object Profile : NavigationItem("profile", R.drawable.ic_eye, "Profile")
    object Chats : NavigationItem("chats", R.drawable.ic_friends, "Chats")
}