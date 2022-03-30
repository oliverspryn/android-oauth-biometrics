package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.biometric.BiometricPrompt
import javax.inject.Inject

class CreateBiometricPromptInfoToPerformBiometricLoginUseCase @Inject constructor(
    private val obtainStrongestAvailableAuthenticationTypeForCryptographyUseCase: ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
) {

    operator fun invoke(): BiometricPrompt.PromptInfo {
        val promptRequestType = obtainStrongestAvailableAuthenticationTypeForCryptographyUseCase()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Log into Your Account")
            .setSubtitle("Using biometrics")
            .setDescription("Use your biometric information to quickly log into your account")

        if (promptRequestType is StrongestAvailableAuthenticationTypeForCryptography.Available) {
            promptInfo.setAllowedAuthenticators(promptRequestType.authenticators)
            promptInfo.setConfirmationRequired(true)

            if (!promptRequestType.allowsDeviceCredentials) {
                promptInfo.setNegativeButtonText("Cancel")
            }
        }

        return promptInfo.build()
    }
}
