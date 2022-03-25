package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.inject.Inject

class PresentBiometricPromptForCipherUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        cipher: Cipher,
        onSuccess: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onFailed: () -> Unit,
        onError: (errorCode: Int, description: String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
