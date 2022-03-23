package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CreateBiometricPromptInfoForLoginUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(): BiometricPrompt.PromptInfo {
        val promptRequestType = generateMostSecureAuthenticatorTypeRequest()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sign into the App")
            .setSubtitle("Using biometrics")
            .setDescription("You can use your fingerprint to sign back into the app")

        if (promptRequestType is BestAuthenticatorType.Available) {
            promptInfo.setAllowedAuthenticators(promptRequestType.bitmask)
            promptInfo.setConfirmationRequired(true)

            if (!promptRequestType.allowsDeviceCredentials) {
                promptInfo.setNegativeButtonText("Cancel")
            }
        }

        return promptInfo.build()
    }

    /**
     * Checks in order of preference what kinds of authenticators the user
     * may use.
     *
     * Tested in decreasing order of preference:
     *   - BIOMETRIC_STRONG or DEVICE_CREDENTIAL
     *   - BIOMETRIC_STRONG
     *   - BIOMETRIC_WEAK or DEVICE_CREDENTIAL
     *   - BIOMETRIC_WEAK
     *   - DEVICE_CREDENTIAL
     *
     * More information is available here:
     *   - https://developer.android.com/training/sign-in/biometric-auth#declare-supported-authentication-types
     *   - https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)
     *   - https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setAllowedAuthenticators(int)
     *   - https://developer.android.com/reference/androidx/biometric/BiometricManager.Authenticators#constants_1
     */
    private fun generateMostSecureAuthenticatorTypeRequest(): BestAuthenticatorType {
        val biometricManager = BiometricManager.from(context)
        val authenticatorsToTry = arrayListOf(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            BIOMETRIC_STRONG,
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL,
            BIOMETRIC_WEAK,
            DEVICE_CREDENTIAL
        )

        authenticatorsToTry.forEach { authenticator ->
            val outcome = biometricManager.mapToSupportedAuthenticatorType(authenticator)

            // Report on success, nothing enrolled (we need the user to enroll something now)
            // or insecure hardware (we can't use the sensor with a security vulnerability)
            if (outcome !is BestAuthenticatorType.NotAvailable) {
                return outcome
            }
        }

        return BestAuthenticatorType.NotAvailable
    }
}

private sealed interface BestAuthenticatorType {
    data class Available(val bitmask: Int) : BestAuthenticatorType {
        val allowsDeviceCredentials: Boolean
            get() = bitmask and DEVICE_CREDENTIAL != 0
    }

    object InsecureHardware : BestAuthenticatorType
    object NotAvailable : BestAuthenticatorType
    object NothingEnrolled : BestAuthenticatorType
}

private fun BiometricManager.mapToSupportedAuthenticatorType(authenticators: Int): BestAuthenticatorType {
    return when (canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BestAuthenticatorType.Available(authenticators)
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BestAuthenticatorType.InsecureHardware
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BestAuthenticatorType.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BestAuthenticatorType.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BestAuthenticatorType.NotAvailable
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BestAuthenticatorType.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BestAuthenticatorType.NothingEnrolled
        else -> BestAuthenticatorType.NotAvailable
    }
}
