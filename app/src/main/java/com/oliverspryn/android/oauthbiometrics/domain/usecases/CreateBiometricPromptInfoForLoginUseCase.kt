package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.biometric.BiometricPrompt
import javax.inject.Inject

class CreateBiometricPromptInfoForLoginUseCase @Inject constructor(
    private val obtainStrongestAvailableAuthenticationTypeUseCase: ObtainStrongestAvailableAuthenticationTypeUseCase
) {

    operator fun invoke(): BiometricPrompt.PromptInfo {
        val promptRequestType = obtainStrongestAvailableAuthenticationTypeUseCase()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sign into the App")
            .setSubtitle("Using biometrics")
            .setDescription("You can use your fingerprint to sign back into the app")

        if (promptRequestType is StrongestAvailableAuthenticationType.Available) {
            promptInfo.setAllowedAuthenticators(promptRequestType.authenticators)
            promptInfo.setConfirmationRequired(true)

            if (!promptRequestType.allowsDeviceCredentials) {
                promptInfo.setNegativeButtonText("Cancel")
            }
        }

        return promptInfo.build()
    }
}
