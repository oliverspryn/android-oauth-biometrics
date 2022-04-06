package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.fragment.app.FragmentActivity
import com.oliverspryn.android.oauthbiometrics.model.StartUiState
import com.oliverspryn.android.oauthbiometrics.model.StartViewModel

@Composable
fun StartScreen(
    activity: FragmentActivity,
    startViewModel: StartViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by startViewModel.uiState.collectAsState()

    StartScreen(
        uiState = uiState,
        onBiometricLockoutConfirmed = { startViewModel.dismissBiometricLockoutRationalePrompt() },
        onBiometricLoginTap = { startViewModel.doBiometricLogin(activity, onLoginSuccess) },
        onWebLoginConfirmed = { startViewModel.dismissWebLoginRationalePrompt() },
        onWebLoginTap = { startViewModel.doWebLogin() }
    )
}

@Composable
fun StartScreen(
    uiState: StartUiState,
    onBiometricLockoutConfirmed: () -> Unit,
    onBiometricLoginTap: () -> Unit,
    onWebLoginConfirmed: () -> Unit,
    onWebLoginTap: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val (buttons, loader) = createRefs()

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.constrainAs(loader) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.constrainAs(buttons) {
                centerHorizontallyTo(parent)
                centerVerticallyTo(parent)
            }
        ) {
            Button(
                enabled = uiState.isWebLoginEnabled,
                onClick = onWebLoginTap
            ) {
                Text(text = "Login with Web")
            }

            Button(
                enabled = uiState.isBiometricLoginEnabled,
                onClick = onBiometricLoginTap
            ) {
                Text(text = "Login with Biometrics")
            }
        }
    }

    if (uiState.showBiometricLockoutRationalePrompt) {
        BiometricLockoutRationalePrompt(
            onBiometricLockoutConfirmed = onBiometricLockoutConfirmed
        )
    }

    if (uiState.showWebLoginRationalePrompt) {
        WebLoginRationalePrompt(
            onWebLoginConfirmed = onWebLoginConfirmed
        )
    }
}

@Composable
private fun BiometricLockoutRationalePrompt(
    onBiometricLockoutConfirmed: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = "Login attempts exceeded")
        },
        text = {
            Text(text = "You have exceeded the maximum allowed biometric login attempts. Please log in through the web or try again later.")
        },
        confirmButton = {
            TextButton(
                onClick = onBiometricLockoutConfirmed
            ) {
                Text("OK")
            }
        },
        onDismissRequest = onBiometricLockoutConfirmed
    )
}

@Composable
private fun WebLoginRationalePrompt(
    onWebLoginConfirmed: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = "Biometric login disabled")
        },
        text = {
            Text(text = "The biometric settings on your device have changed. For security purposes, you will need to login again through the web.")
        },
        confirmButton = {
            TextButton(
                onClick = onWebLoginConfirmed
            ) {
                Text("OK")
            }
        },
        onDismissRequest = onWebLoginConfirmed
    )
}

@Preview(showSystemUi = true)
@Composable
fun PreviewStartScreen() {
    StartScreen(
        uiState = StartUiState(),
        onBiometricLockoutConfirmed = {},
        onBiometricLoginTap = {},
        onWebLoginConfirmed = {},
        onWebLoginTap = {}
    )
}
