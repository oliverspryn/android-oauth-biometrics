package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.security.keystore.KeyGenParameterSpec
import javax.inject.Inject

class KeyGenParameterSpecBuilderForwarder @Inject constructor() {
    fun builder(
        keystoreAlias: String,
        purposes: Int
    ) = KeyGenParameterSpec.Builder(
        keystoreAlias,
        purposes
    )
}
