package com.oliverspryn.android.oauthbiometrics.ui.loading

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oliverspryn.android.oauthbiometrics.model.LoadingUiState
import com.oliverspryn.android.oauthbiometrics.model.LoadingViewModel
import com.oliverspryn.android.oauthbiometrics.ui.theme.OAuthBiometricsTheme
import com.oliverspryn.android.oauthbiometrics.utils.RunOnceEffect

@Composable
fun LoadingScreen(
    activity: ComponentActivity,
    loadingViewModel: LoadingViewModel,
    notLoggingIn: () -> Unit,
    loginSuccess: () -> Unit
) {
    val uiState by loadingViewModel.uiState.collectAsState()

    RunOnceEffect {
        loadingViewModel.performTokenExchange(
            activity = activity,
            notLoggingIn = notLoggingIn,
            loginSuccess = loginSuccess
        )
    }

    LoadingScreen(
        uiState = uiState
    )
}

@Composable
fun LoadingScreen(
    uiState: LoadingUiState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = uiState.message)
    }
}

@Composable
@Preview(showSystemUi = true)
fun PreviewLoadingScreen() {
    OAuthBiometricsTheme {
        LoadingScreen(
            uiState = LoadingUiState(
                message = "Hi"
            )
        )
    }
}
