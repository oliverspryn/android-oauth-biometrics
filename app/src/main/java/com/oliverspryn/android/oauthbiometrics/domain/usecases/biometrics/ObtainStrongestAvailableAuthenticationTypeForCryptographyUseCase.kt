package com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.oliverspryn.android.oauthbiometrics.di.forwarders.BiometricManagerForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig
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
     *   - DEVICE_CREDENTIAL
     *
     *  Tested in decreasing order of preference for < API 30:
     *   - BIOMETRIC_STRONG
     *   - BIOMETRIC_WEAK
     *
     * Only API 30+ supports PIN/pattern/password with CryptoObjects. Hence,
     * the two conditions defined above.
     *
     * If the ALLOW_DEVICE_CREDENTIALS_AS_BIOMETRIC_OPTION is true, the
     * first list above runs. If it is false, then only BIOMETRIC_STRONG
     * is presented.
     *
     * Per the setUserAuthenticationParameters() function on the
     * KeyGenParameterSpec.Builder, only BIOMETRIC_STRONG and DEVICE_CREDENTIAL
     * are available. Thus, BIOMETRIC_WEAK is omitted for all API 30+ devices
     * to comply with that requirement. Older API levels offer BIOMETRIC_WEAK
     * since BIOMETRIC_STRONG is a newer standardization per the first link below,
     * and may not be available on older devices. Thus, for maximum backward
     * compatibility, BIOMETRIC_WEAK is the final option presented for < API 30
     * devices. Note that setUserAuthenticationParameters() is only an API 30+
     * function, but its predecessor setUserAuthenticationValidityDurationSeconds()
     * makes the same call under the hood with the modern APIs. So, its in a
     * quasi modern/legacy state on older API levels, and thus the need for
     * both STRONG and WEAK offerings.
     *
     * More information is available here:
     *   - https://developer.android.com/training/sign-in/biometric-auth#declare-supported-authentication-types
     *   - https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)
     *   - https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setAllowedAuthenticators(int)
     *   - https://developer.android.com/reference/androidx/biometric/BiometricManager.Authenticators#constants_1
     */
    operator fun invoke(): StrongestAvailableAuthenticationTypeForCryptography {
        val biometricManager = biometricManagerForwarder.from(context)
        val authenticatorsToTry =
            if (sdkInt >= Build.VERSION_CODES.R && CryptographyConfig.ALLOW_DEVICE_CREDENTIALS_AS_BIOMETRIC_OPTION) {
                arrayListOf(
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
                    BIOMETRIC_STRONG,
                    DEVICE_CREDENTIAL
                )
            } else if (sdkInt >= Build.VERSION_CODES.R && !CryptographyConfig.ALLOW_DEVICE_CREDENTIALS_AS_BIOMETRIC_OPTION) {
                arrayListOf(
                    BIOMETRIC_STRONG
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
    data class Available(val authenticators: Int) :
        StrongestAvailableAuthenticationTypeForCryptography {
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
