package com.hwx.chatique

object NavigationKeys {

    object Route {
        const val LOGIN = "login"
        const val SIGN_UP = "sign_up"
        const val USER_SEARCH = "user_search"
        const val USER_DETAILS = "user_details"
        const val USER_DETAILS_ROUTE = "$USER_DETAILS/{${Arg.USER_IDS}}/{${Arg.USERNAME}}"
        const val SET_PIN = "set_pin"

        const val CHAT = "chat"
        const val CHAT_ROUTE =
            "$CHAT?${Arg.USER_IDS}={${Arg.USER_IDS}}&${Arg.CHAT_ID}={${Arg.CHAT_ID}}&${Arg.IS_SECRET}={${Arg.IS_SECRET}}"

        const val GROUP_CHAT_CREATION = "GROUP_CHAT_CREATION"
        const val GROUP_CHAT_CREATION_ROUTE =
            "$GROUP_CHAT_CREATION?${Arg.IS_SECRET}={${Arg.IS_SECRET}}"

        const val COMMUNICATION_ROOM = "COMMUNICATION_ROOM"
        const val COMMUNICATION_ROOM_ROUTE =
            "$COMMUNICATION_ROOM?${Arg.CHAT_ID}={${Arg.CHAT_ID}}&${Arg.USER_FROM_ID}={${Arg.USER_FROM_ID}}&${Arg.COMM_ROOM_SCREEN_INITIAL}={${Arg.COMM_ROOM_SCREEN_INITIAL}}"
    }

    object Arg {
        const val USER_IDS = "userIds"
        const val USERNAME = "userName"
        const val CHAT_ID = "chatId"
        const val IS_SECRET = "isSecret"
        const val IS_VOICE_CALL_ACCEPTED = "isVoiceCallAccepted"
        const val USER_FROM_ID = "userFromId"
        const val COMM_ROOM_SCREEN_INITIAL = "COMM_ROOM_SCREEN_INITIAL"
    }
}