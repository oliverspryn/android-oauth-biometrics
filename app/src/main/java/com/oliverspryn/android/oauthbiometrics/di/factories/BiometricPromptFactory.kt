package com.oliverspryn.android.oauthbiometrics.di.factories

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import javax.inject.Inject

class BiometricPromptFactory @Inject constructor() {

    fun newInstance(
        activity: FragmentActivity,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback
    ) = BiometricPrompt(
        activity,
        executor,
        callback
    )
}
