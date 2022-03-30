package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
        onBiometricLoginTap = { startViewModel.doBiometricLogin(activity, onLoginSuccess) },
        onWebLoginTap = { startViewModel.doWebLogin() }
    )
}

@Composable
fun StartScreen(
    uiState: StartUiState,
    onBiometricLoginTap: () -> Unit,
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
}

@Preview(showSystemUi = true)
@Composable
fun PreviewStartScreen() {
    StartScreen(
        uiState = StartUiState(),
        onBiometricLoginTap = {},
        onWebLoginTap = {}
    )
}
