package com.oliverspryn.android.oauthbiometrics.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oliverspryn.android.oauthbiometrics.model.StartUiState
import com.oliverspryn.android.oauthbiometrics.model.StartViewModel

@Composable
fun StartScreen(
    startViewModel: StartViewModel,
    onLoginTap: () -> Unit,
    onReauthTap: () -> Unit
) {
    val uiState by startViewModel.uiState.collectAsState()

    StartScreen(
        uiState = uiState,
        onLoginTap = onLoginTap,
        onReauthTap = onReauthTap
    )
}

@Composable
fun StartScreen(
    uiState: StartUiState,
    onLoginTap: () -> Unit,
    onReauthTap: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onLoginTap) {
                Text(text = "Login")
            }

            Button(
                enabled = uiState.isReauthEnabled,
                onClick = onReauthTap
            ) {
                Text(text = "Reauthenticate with Biometrics")
            }
        }
    }
}

@Preview
@Composable
fun PreviewStartScreen() {
    StartScreen(
        uiState = StartUiState(),
        onLoginTap = {},
        onReauthTap = {}
    )
}
