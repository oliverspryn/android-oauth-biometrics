package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.oliverspryn.android.oauthbiometrics.di.forwarders.BiometricManagerForwarder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ObtainStrongestAvailableAuthenticationTypeUseCase @Inject constructor(
    private val biometricManagerForwarder: BiometricManagerForwarder,
    @ApplicationContext private val context: Context
) {

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
    operator fun invoke(): StrongestAvailableAuthenticationType {
        val biometricManager = biometricManagerForwarder.from(context)
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
            // Basically react on success or when the user needs to take some kind of action
            // to enable the suggested authenticator
            // The only thing we can't do anything about is NotAvailable
            if (outcome !is StrongestAvailableAuthenticationType.NotAvailable) {
                return outcome
            }
        }

        return StrongestAvailableAuthenticationType.NotAvailable
    }
}

sealed interface StrongestAvailableAuthenticationType {
    data class Available(val authenticators: Int) : StrongestAvailableAuthenticationType {
        val allowsDeviceCredentials: Boolean
            get() = authenticators and DEVICE_CREDENTIAL != 0

        val hasBiometricSupport: Boolean
            get() = authenticators and (BIOMETRIC_STRONG or BIOMETRIC_WEAK) != 0
    }

    object InsecureHardware : StrongestAvailableAuthenticationType
    object NotAvailable : StrongestAvailableAuthenticationType
    object NothingEnrolled : StrongestAvailableAuthenticationType
}

private fun BiometricManager.mapToSupportedAuthenticatorType(authenticators: Int): StrongestAvailableAuthenticationType {
    return when (canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> StrongestAvailableAuthenticationType.Available(
            authenticators
        )

        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> StrongestAvailableAuthenticationType.InsecureHardware
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> StrongestAvailableAuthenticationType.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> StrongestAvailableAuthenticationType.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> StrongestAvailableAuthenticationType.NotAvailable
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> StrongestAvailableAuthenticationType.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> StrongestAvailableAuthenticationType.NothingEnrolled
        else -> StrongestAvailableAuthenticationType.NotAvailable
    }
}
