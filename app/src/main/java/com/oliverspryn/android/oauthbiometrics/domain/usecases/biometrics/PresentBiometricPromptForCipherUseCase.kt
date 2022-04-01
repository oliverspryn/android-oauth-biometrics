package com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.oliverspryn.android.oauthbiometrics.di.factories.BiometricPromptCryptoObjectFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.BiometricPromptFactory
import com.oliverspryn.android.oauthbiometrics.di.forwarders.ContextCompatForwarder
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import javax.crypto.Cipher
import javax.inject.Inject

class PresentBiometricPromptForCipherUseCase @Inject constructor(
    private val biometricPromptFactory: BiometricPromptFactory,
    private val biometricPromptCryptoObjectFactory: BiometricPromptCryptoObjectFactory,
    @ApplicationContext private val context: Context,
    private val contextCompatForwarder: ContextCompatForwarder
) {

    operator fun invoke(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        cipher: Cipher
    ): Observable<BiometricResult> = Observable.create { observableEmitter ->
        val executor = contextCompatForwarder.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                observableEmitter.onError(
                    BiometricResult.Error(
                        errorCode = errorCode,
                        errorString = errString.toString(),
                        isBiometricLockout = errorCode.isBiometricLockout()
                    )
                )
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                observableEmitter.onNext(BiometricResult.Failed)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                observableEmitter.onNext(BiometricResult.Success(result))
                observableEmitter.onComplete()
            }
        }

        val prompt = biometricPromptFactory.newInstance(activity, executor, callback)
        prompt.authenticate(promptInfo, biometricPromptCryptoObjectFactory.newInstance(cipher))
    }
}

sealed interface BiometricResult {
    data class Error(
        val errorCode: Int,
        val errorString: String,
        val isBiometricLockout: Boolean
    ) : BiometricResult, Throwable(errorString)

    object Failed : BiometricResult
    data class Success(val result: BiometricPrompt.AuthenticationResult) : BiometricResult
}

private fun Int.isBiometricLockout(): Boolean =
    this == BiometricPrompt.ERROR_LOCKOUT || this == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
