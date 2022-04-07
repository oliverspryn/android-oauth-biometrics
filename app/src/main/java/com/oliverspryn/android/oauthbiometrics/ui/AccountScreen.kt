package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.oliverspryn.android.oauthbiometrics.model.AccountUiState
import com.oliverspryn.android.oauthbiometrics.model.AccountViewModel

@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel,
    activity: FragmentActivity
) {
    val uiState by accountViewModel.uiState.collectAsState()
    accountViewModel.updateBiometricsOptionAvailability()

    AccountScreen(
        uiState = uiState,
        onBiometricLockoutConfirmed = { accountViewModel.dismissBiometricLockoutRationalePrompt() },
        onBiometricLoginEnabled = { isEnabled ->
            accountViewModel.setBiometricLoginFeatureEnabled(
                isEnabled,
                activity
            )
        },
        onDeviceEnrollmentConfirmed = {
            accountViewModel.dismissDeviceEnrollmentPrompt()
            accountViewModel.goToAndroidSecuritySettings()
        },
        onDeviceEnrollmentDismissed = { accountViewModel.dismissDeviceEnrollmentPrompt() },
        onEnrollBiometricsTap = { accountViewModel.enrollBiometrics() },
        onLogoutTap = { accountViewModel.logout(activity) }
    )
}

@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onBiometricLockoutConfirmed: () -> Unit,
    onBiometricLoginEnabled: (Boolean) -> Unit,
    onDeviceEnrollmentConfirmed: () -> Unit,
    onDeviceEnrollmentDismissed: () -> Unit,
    onEnrollBiometricsTap: () -> Unit,
    onLogoutTap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.userInfo is AccountUiState.UserInfoResponse.WithData) {
            Text(
                text = "Hello ${uiState.userInfo.payload.name}, you are logged in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = uiState.userInfo.payload.picture,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .clip(CircleShape)
                    .width(200.dp)
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (uiState.isBiometricLoginFeatureAvailable) {
            Text(
                text = "Enable Biometric Login?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Available authentication classifiers:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = uiState.supportedBiometricClassifiers,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Switch(
                checked = uiState.isBiometricLoginOptionChecked,
                onCheckedChange = onBiometricLoginEnabled
            )
        } else if (!uiState.userNeedsToRegisterDeviceSecurity) {
            Text(
                text = "Biometric login not available on your device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.userNeedsToRegisterDeviceSecurity) {
            Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogoutTap
        ) {
            Text(text = "Logout")
        }
    }

    if (uiState.showBiometricLockoutRationalePrompt) {
        BiometricLockoutRationalePrompt(
            onBiometricLockoutConfirmed = onBiometricLockoutConfirmed
        )
    }

    if (uiState.showDeviceSecurityEnrollmentPrompt) {
        DeviceSecurityEnrollmentPrompt(
            onDeviceEnrollmentConfirmed = onDeviceEnrollmentConfirmed,
            onDeviceEnrollmentDismissed = onDeviceEnrollmentDismissed
        )
    }
}

@Composable
private fun BiometricLockoutRationalePrompt(
    onBiometricLockoutConfirmed: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = "Confirmation attempts exceeded")
        },
        text = {
            Text(text = "You have exceeded the maximum allowed biometric confirmation attempts. Please try again later.")
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
private fun DeviceSecurityEnrollmentPrompt(
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
