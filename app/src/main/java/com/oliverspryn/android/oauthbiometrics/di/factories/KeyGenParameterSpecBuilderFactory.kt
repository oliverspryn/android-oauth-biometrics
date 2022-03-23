package com.oliverspryn.android.oauthbiometrics.di.factories

import android.security.keystore.KeyGenParameterSpec
import javax.inject.Inject

class KeyGenParameterSpecBuilderFactory @Inject constructor() {
    fun build(
        keystoreAlias: String,
        purposes: Int
    ) = KeyGenParameterSpec.Builder(
        keystoreAlias,
        purposes
    )
}
