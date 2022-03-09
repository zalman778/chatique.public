package com.hwx.chatique.ui.signUp

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
import com.hwx.chatique.ui.signUp.SignUpContract.Companion.isSignUpFormValid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@ExperimentalComposeUiApi
@Composable
fun SignUpScreen(
    state: SignUpContract.State,
    effectFlow: Flow<SignUpContract.Effect>?,
    onEventSent: (event: SignUpContract.Event) -> Unit,
    onNavigationRequested: (navigationEffect: SignUpContract.Effect.Navigation) -> Unit,
) {
    LaunchedEffect(LAUNCH_LISTEN_FOR_EFFECTS) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is SignUpContract.Effect.Navigation.GoToApplication -> onNavigationRequested(
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
    onEventSent: (event: SignUpContract.Event) -> Unit,
    state: SignUpContract.State,
    onNavigationRequested: (navigationEffect: SignUpContract.Effect.Navigation) -> Unit,
) {
    val login = state.login
    val password = state.password
    val bio = state.bio
    val email = state.email
    val isSignUpEnabled = state.isSignUpFormValid()

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
                        onEventSent(SignUpContract.Event.LoginChanged(it))
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
                        onEventSent(SignUpContract.Event.PasswordChanged(it))
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

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                elevation = 4.dp,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, Shapes.medium),
                    value = bio,
                    onValueChange = {
                        onEventSent(SignUpContract.Event.BioChanged(it))
                    },
                    label = { Text(text = stringResource(id = R.string.bio)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusRequester.requestFocus() }
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                elevation = 4.dp,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, Shapes.medium),
                    value = email,
                    onValueChange = {
                        onEventSent(SignUpContract.Event.EmailChanged(it))
                    },
                    label = { Text(text = stringResource(id = R.string.email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusRequester.requestFocus() }
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.state == ScreenState.RETRY) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.errorStr, color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, Shapes.medium)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    onEventSent(SignUpContract.Event.OnSignUpClick)
                },
                enabled = isSignUpEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.sign_up))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    onNavigationRequested(SignUpContract.Effect.Navigation.GoToLogin)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.go_to_login))
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