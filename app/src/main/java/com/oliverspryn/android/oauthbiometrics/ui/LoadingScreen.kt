package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.oliverspryn.android.oauthbiometrics.model.LoadingUiState
import com.oliverspryn.android.oauthbiometrics.model.LoadingViewModel
import com.oliverspryn.android.oauthbiometrics.ui.theme.OAuthBiometricsTheme
import com.oliverspryn.android.oauthbiometrics.utils.RunOnceEffect

@Composable
fun LoadingScreen(
    activity: FragmentActivity,
    loadingViewModel: LoadingViewModel,
    onNotLoggingIn: () -> Unit,
    onLoginSuccess: () -> Unit,
    onRestartFlow: () -> Unit
) {
    val uiState by loadingViewModel.uiState.collectAsState()

    RunOnceEffect {
        loadingViewModel.performTokenExchange(
            activity = activity,
            onNotLoggingIn = onNotLoggingIn,
            onLoginSuccess = onLoginSuccess
        )
    }

    LoadingScreen(
        uiState = uiState,
        onRestartFlow = onRestartFlow
    )
}

@Composable
fun LoadingScreen(
    uiState: LoadingUiState,
    onRestartFlow: () -> Unit
) {
    if (uiState.message.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRestartFlow
            ) {
                Text(text = "Restart Flow")
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true)
fun PreviewLoadingScreen() {
    OAuthBiometricsTheme {
        LoadingScreen(
            uiState = LoadingUiState(
                message = "Hi"
            ),
            onRestartFlow = {}
        )
    }
}
