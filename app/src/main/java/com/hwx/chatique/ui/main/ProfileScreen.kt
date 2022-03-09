package com.hwx.chatique.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hwx.chatique.NavigationKeys
import com.hwx.chatique.flow.IAuthInteractor
import com.hwx.chatique.flow.IProfileHolder
import com.hwx.chatique.p2p.IE2eController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    authInteractor: IAuthInteractor,
    navController: NavHostController,
    dhCoordinator: IE2eController,
    profileHolder: IProfileHolder,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = "Profile View",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = profileHolder.username,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Start),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            GlobalScope.launch {
                authInteractor.logout()
                dhCoordinator.logout()
                withContext(Dispatchers.Main) {
                    navController.navigate(NavigationKeys.Route.LOGIN)
                }
            }
        }) {
            Text("logout")
        }
    }
}