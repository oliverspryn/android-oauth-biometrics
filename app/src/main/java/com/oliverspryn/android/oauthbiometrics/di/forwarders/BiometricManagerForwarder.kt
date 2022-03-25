package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.content.Context
import androidx.biometric.BiometricManager
import javax.inject.Inject

class BiometricManagerForwarder @Inject constructor() {
    fun from(context: Context) = BiometricManager.from(context)
}
