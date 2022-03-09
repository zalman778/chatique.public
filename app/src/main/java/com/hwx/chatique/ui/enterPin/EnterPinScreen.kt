package com.hwx.chatique.ui.enterPin

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwx.chatique.flow.IAuthInteractor

@Composable
fun EnterPinScreen(
    authInteractor: IAuthInteractor,
    isSettingPin: Boolean = true,
) {
    var pin by remember { mutableStateOf("") }
    val onDigitClick = lit@{ it: String ->
        if (pin.length > 3) return@lit
        pin += it
        if (pin.length == 4) {
            if (isSettingPin) {
                authInteractor.setPin(pin)
            } else {
                val isValid = authInteractor.validatePin(pin)
                if (!isValid) {
                    pin = ""
                }
            }
        }
    }
    Column {
        Text(
            text = pin,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.height(100.dp),
        ) {
            DigitButton("1", onDigitClick)
            DigitButton("2", onDigitClick)
            DigitButton("3", onDigitClick)
        }

        Row(
            modifier = Modifier.height(100.dp),
        ) {
            DigitButton("4", onDigitClick)
            DigitButton("5", onDigitClick)
            DigitButton("6", onDigitClick)
        }

        Row(
            modifier = Modifier.height(100.dp),
        ) {
            DigitButton("7", onDigitClick)
            DigitButton("8", onDigitClick)
            DigitButton("9", onDigitClick)
        }

        Row(
            modifier = Modifier.height(100.dp),
        ) {
            DigitButton("0", onDigitClick)
            if (authInteractor.isBiometricsAvailable && !isSettingPin) {
                TextButton(
                    onClick = {
                        authInteractor.startBiometricsAuth()
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f),
                ) {
                    Text("finger")
                }
            }
        }
    }
}

@Composable
private fun RowScope.DigitButton(digit: String, onClick: (String) -> Unit) {
    TextButton(
        onClick = { onClick(digit) },
        modifier = Modifier
            .padding(8.dp)
            .weight(1f),
    ) {
        Text(digit)
    }
}