package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.biometric.BiometricPrompt
import javax.inject.Inject

class CreateBiometricPromptInfoForEnableReauthenticationUseCase @Inject constructor(
    private val obtainStrongestAvailableAuthenticationTypeUseCase: ObtainStrongestAvailableAuthenticationTypeUseCase
) {

    operator fun invoke(): BiometricPrompt.PromptInfo {
        val promptRequestType = obtainStrongestAvailableAuthenticationTypeUseCase()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable biometric login")
            .setSubtitle("Using biometrics")
            .setDescription("Confirm your choice to enable a biometric login")

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
