package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.oliverspryn.android.oauthbiometrics.di.forwarders.BiometricManagerForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named

class ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase @Inject constructor(
    private val biometricManagerForwarder: BiometricManagerForwarder,
    @ApplicationContext private val context: Context,
    @Named(BuildModule.SDK_INT) private val sdkInt: Int
) {

    /**
     * Checks in order of preference what kinds of authenticators the user
     * may use.
     *
     * Tested in decreasing order of preference for >= API 30:
     *   - BIOMETRIC_STRONG or DEVICE_CREDENTIAL
     *   - BIOMETRIC_STRONG
     *   - BIOMETRIC_WEAK or DEVICE_CREDENTIAL
     *   - BIOMETRIC_WEAK
     *   - DEVICE_CREDENTIAL
     *
     *  Tested in decreasing order of preference for < API 30:
     *   - BIOMETRIC_STRONG
     *   - BIOMETRIC_WEAK
     *
     * Only API 30+ supports PIN/pattern/password with CryptoObjects. Hence,
     * the two conditions defined above.
     *
     * More information is available here:
     *   - https://developer.android.com/training/sign-in/biometric-auth#declare-supported-authentication-types
     *   - https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)
     *   - https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setAllowedAuthenticators(int)
     *   - https://developer.android.com/reference/androidx/biometric/BiometricManager.Authenticators#constants_1
     */
    operator fun invoke(): StrongestAvailableAuthenticationTypeForCryptography {
        val biometricManager = biometricManagerForwarder.from(context)
        val authenticatorsToTry = if (sdkInt >= Build.VERSION_CODES.R) {
            arrayListOf(
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
                BIOMETRIC_STRONG,
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL,
                BIOMETRIC_WEAK,
                DEVICE_CREDENTIAL
            )
        } else {
            arrayListOf(
                BIOMETRIC_STRONG,
                BIOMETRIC_WEAK
            )
        }

        authenticatorsToTry.forEach { authenticator ->
            val outcome = biometricManager.mapToSupportedAuthenticatorType(authenticator)

            // Report on success, nothing enrolled (we need the user to enroll something now)
            // or insecure hardware (we can't use the sensor with a security vulnerability)
            // Basically react on success or when the user needs to take some kind of action
            // to enable the suggested authenticator
            // The only thing we can't do anything about is NotAvailable
            if (outcome !is StrongestAvailableAuthenticationTypeForCryptography.NotAvailable) {
                return outcome
            }
        }

        return StrongestAvailableAuthenticationTypeForCryptography.NotAvailable
    }
}

sealed interface StrongestAvailableAuthenticationTypeForCryptography {
    data class Available(val authenticators: Int) : StrongestAvailableAuthenticationTypeForCryptography {
        val allowsDeviceCredentials: Boolean
            get() = authenticators and DEVICE_CREDENTIAL != 0
    }

    object InsecureHardware : StrongestAvailableAuthenticationTypeForCryptography
    object NotAvailable : StrongestAvailableAuthenticationTypeForCryptography
    object NothingEnrolled : StrongestAvailableAuthenticationTypeForCryptography
}

private fun BiometricManager.mapToSupportedAuthenticatorType(authenticators: Int): StrongestAvailableAuthenticationTypeForCryptography {
    return when (canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> StrongestAvailableAuthenticationTypeForCryptography.Available(
            authenticators
        )

        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> StrongestAvailableAuthenticationTypeForCryptography.InsecureHardware
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> StrongestAvailableAuthenticationTypeForCryptography.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> StrongestAvailableAuthenticationTypeForCryptography.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> StrongestAvailableAuthenticationTypeForCryptography.NotAvailable
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> StrongestAvailableAuthenticationTypeForCryptography.NotAvailable
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
        else -> StrongestAvailableAuthenticationTypeForCryptography.NotAvailable
    }
}
