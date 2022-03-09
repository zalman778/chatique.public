package com.hwx.chatique.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hwx.chatique.R
import com.hwx.chatique.arch.LAUNCH_LISTEN_FOR_EFFECTS
import com.hwx.chatique.arch.ScreenState
import com.hwx.chatique.theme.Shapes
import com.hwx.chatique.ui.login.LoginContract.Companion.isLoginFormValid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@ExperimentalComposeUiApi
@Composable
fun LoginScreen(
    state: LoginContract.State,
    effectFlow: Flow<LoginContract.Effect>?,
    onEventSent: (event: LoginContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: LoginContract.Effect.Navigation) -> Unit,
) {
    LaunchedEffect(LAUNCH_LISTEN_FOR_EFFECTS) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is LoginContract.Effect.Navigation.GoToApplication -> onNavigationRequested(
                    effect
                )
            }
        }?.collect()
    }

    when (state.state) {
        ScreenState.INITIAL, ScreenState.RETRY, ScreenState.LOADING -> LoginForm(
            onEventSent,
            state,
            onNavigationRequested,
        )
    }
}

@ExperimentalComposeUiApi
@Composable
private fun LoginForm(
    onEventSent: (event: LoginContract.Event) -> Unit,
    state: LoginContract.State,
    onNavigationRequested: (navigationEffect: LoginContract.Effect.Navigation) -> Unit,
) {
    val login = state.login
    val password = state.password
    val isLoginButtonEnabled = state.isLoginFormValid()

    Box {
        Image(
            painter = painterResource(R.drawable.bg_raindrop),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .matchParentSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val (focusRequester) = FocusRequester.createRefs()

            Surface(
                elevation = 4.dp,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, Shapes.medium),
                    value = login,
                    onValueChange = {
                        onEventSent(LoginContract.Event.LoginChanged(it))
                    },
                    label = { Text(text = stringResource(id = R.string.login)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusRequester.requestFocus() }
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            var isPasswordVisible: Boolean by remember { mutableStateOf(false) }

            Surface(
                elevation = 4.dp,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, Shapes.medium)
                        .focusRequester(focusRequester),
                    value = password,
                    onValueChange = {
                        onEventSent(LoginContract.Event.PasswordChanged(it))
                    },
                    visualTransformation = if (isPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            isPasswordVisible = !isPasswordVisible
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_eye), "",
                                colorFilter = ColorFilter.tint(Color.Black)
                            )
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.password)) },
                    singleLine = true,
                    shape = Shapes.medium,
                )
            }

            if (state.state == ScreenState.RETRY) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, Shapes.medium),
                ) {
                    Text(
                        text = state.errorStr,
                        color = Color.Red,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {

                    onEventSent(LoginContract.Event.OnLoginClick)
                },
                enabled = isLoginButtonEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.login))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    onNavigationRequested(LoginContract.Effect.Navigation.GoToSignUp)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.go_to_sign_up))
            }

            if (state.state == ScreenState.LOADING) {
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
        }
    }
}