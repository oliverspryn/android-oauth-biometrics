package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.biometric.BiometricPrompt
import javax.inject.Inject

class CreateBiometricPromptInfoForEnableBiometricLoginUseCase @Inject constructor(
    private val obtainStrongestAvailableAuthenticationTypeForCryptographyUseCase: ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
) {

    operator fun invoke(): BiometricPrompt.PromptInfo {
        val promptRequestType = obtainStrongestAvailableAuthenticationTypeForCryptographyUseCase()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable biometric login")
            .setSubtitle("Using biometrics")
            .setDescription("Confirm your choice to enable a biometric login")

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
