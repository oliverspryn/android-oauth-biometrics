package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.oliverspryn.android.oauthbiometrics.model.AccountUiState
import com.oliverspryn.android.oauthbiometrics.model.AccountViewModel

@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel,
    activity: FragmentActivity
) {
    val uiState by accountViewModel.uiState.collectAsState()

    AccountScreen(
        uiState = uiState,
        onReauthenticationEnabled = { isEnabled ->
            accountViewModel.setReauthenticationFeatureEnabled(
                isEnabled,
                activity
            )
        }
    )
}

@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onReauthenticationEnabled: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Logged in!")

        Switch(
            checked = uiState.isReauthenticationOptionChecked,
            enabled = uiState.isReauthenticationFeatureEnabled,
            onCheckedChange = onReauthenticationEnabled
        )
    }
}
