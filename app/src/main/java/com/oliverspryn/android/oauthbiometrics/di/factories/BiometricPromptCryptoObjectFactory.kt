package com.oliverspryn.android.oauthbiometrics.di.factories

import androidx.biometric.BiometricPrompt
import javax.crypto.Cipher
import javax.inject.Inject

class BiometricPromptCryptoObjectFactory @Inject constructor() {
    fun newInstance(cipher: Cipher) = BiometricPrompt.CryptoObject(cipher)
}
