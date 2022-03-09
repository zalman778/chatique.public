package com.hwx.chatique.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import com.hwx.chatique.R

@ExperimentalComposeUiApi
@Composable
@Preview
fun RetryBlock(onRetry: () -> Unit = {}) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {

        val (icRefresh, retryButton) = createRefs()
        Image(
            painterResource(R.drawable.ic_refresh),
            "retry",
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
                .padding(8.dp)
                .constrainAs(icRefresh) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(retryButton.top)
                },
            colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500)),
        )

        TextButton(
            onClick = onRetry,
            modifier = Modifier
                .padding(8.dp)
                .width(200.dp)
                .constrainAs(retryButton) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(icRefresh.bottom)
                    bottom.linkTo(parent.bottom)
                },
        ) {
            Text("Retry")
        }

        createVerticalChain(
            icRefresh,
            retryButton,
            chainStyle = ChainStyle.Packed
        )
    }

}