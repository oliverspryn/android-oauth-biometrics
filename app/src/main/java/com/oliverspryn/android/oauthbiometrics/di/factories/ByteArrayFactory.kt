package com.oliverspryn.android.oauthbiometrics.di.factories

import javax.inject.Inject

class ByteArrayFactory @Inject constructor() {
    fun newInstance(size: Int) = ByteArray(size)
}
