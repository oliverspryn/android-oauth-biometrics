package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    accountViewModel.evaluateBiometricsState()

    AccountScreen(
        uiState = uiState,
        onBiometricLoginEnabled = { isEnabled ->
            accountViewModel.setBiometricLoginFeatureEnabled(
                isEnabled,
                activity
            )
        },
        onDeviceEnrollmentConfirmed = {
            accountViewModel.dismissDeviceEnrollmentDialog()
            accountViewModel.goToAndroidSecuritySettings()
        },
        onDeviceEnrollmentDismissed = { accountViewModel.dismissDeviceEnrollmentDialog() },
        onEnrollBiometricsTap = { accountViewModel.enrollBiometrics() }
    )
}

@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onBiometricLoginEnabled: (Boolean) -> Unit,
    onDeviceEnrollmentConfirmed: () -> Unit,
    onDeviceEnrollmentDismissed: () -> Unit,
    onEnrollBiometricsTap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Logged in!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Switch(
            checked = uiState.isBiometricLoginOptionChecked,
            enabled = uiState.isBiometricLoginFeatureAvailable,
            onCheckedChange = onBiometricLoginEnabled
        )

        if (uiState.userNeedsToRegisterDeviceSecurity) {
            Text(
                text = "You don't have any device security enabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = onEnrollBiometricsTap
            ) {
                Text(text = "Enroll Biometrics")
            }
        }
    }

    if (uiState.showDeviceSecurityEnrollmentDialog) {
        DeviceSecurityEnrollmentDialog(
            onDeviceEnrollmentConfirmed = onDeviceEnrollmentConfirmed,
            onDeviceEnrollmentDismissed = onDeviceEnrollmentDismissed
        )
    }
}

@Composable
private fun DeviceSecurityEnrollmentDialog(
    onDeviceEnrollmentConfirmed: () -> Unit,
    onDeviceEnrollmentDismissed: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = "Device Security")
        },
        text = {
            Text(text = "You need to enable device security before saving your login information.")
        },
        confirmButton = {
            TextButton(
                onClick = onDeviceEnrollmentConfirmed
            ) {
                Text("Enable Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeviceEnrollmentDismissed
            ) {
                Text("Dismiss")
            }
        },
        onDismissRequest = onDeviceEnrollmentDismissed
    )
}
