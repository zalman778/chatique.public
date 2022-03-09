package com.hwx.chatique.ui.common

import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable

@Composable
fun LoadingBlock() {
    AlertDialog(
        onDismissRequest = {

        },
        title = {},
        text = {
            CircularProgressIndicator()
        },
        buttons = {},
    )
}