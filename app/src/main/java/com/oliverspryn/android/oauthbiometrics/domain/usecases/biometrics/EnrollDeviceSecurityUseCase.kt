package com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named

class EnrollDeviceSecurityUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: IntentFactory,
    @Named(BuildModule.SDK_INT) private val sdkInt: Int
) {

    @SuppressLint("NewApi") // Lint can't tell I've accounted for this via DI
    operator fun invoke(): DidHandleEnrollmentInternally {
        if (sdkInt < Build.VERSION_CODES.R) return DidHandleEnrollmentInternally.No

        val enrollIntent = intentFactory
            .newInstance(Settings.ACTION_BIOMETRIC_ENROLL)
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                val allowedTypes =
                    if (CryptographyConfig.ALLOW_DEVICE_CREDENTIALS_AS_BIOMETRIC_OPTION) {
                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                    } else {
                        BIOMETRIC_STRONG
                    }

                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    allowedTypes
                )
            }

        context.startActivity(enrollIntent) // Don't care about the outcome, will re-evaluate on resume
        return DidHandleEnrollmentInternally.Yes
    }
}

sealed interface DidHandleEnrollmentInternally {
    object No : DidHandleEnrollmentInternally
    object Yes : DidHandleEnrollmentInternally
}
