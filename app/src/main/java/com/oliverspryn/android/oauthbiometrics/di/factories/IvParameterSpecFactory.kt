package com.oliverspryn.android.oauthbiometrics.di.factories

import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject

class IvParameterSpecFactory @Inject constructor() {
    fun newInstance(iv: ByteArray) = IvParameterSpec(iv)
}
