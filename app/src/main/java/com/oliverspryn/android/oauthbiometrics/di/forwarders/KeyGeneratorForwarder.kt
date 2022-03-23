package com.oliverspryn.android.oauthbiometrics.di.forwarders

import javax.crypto.KeyGenerator
import javax.inject.Inject

class KeyGeneratorForwarder @Inject constructor() {
    fun getInstance(
        algorithm: String,
        provider: String
    ): KeyGenerator = KeyGenerator.getInstance(
        algorithm,
        provider
    )
}
