package com.oliverspryn.android.oauthbiometrics.di.factories

import java.security.SecureRandom
import javax.inject.Inject

class SecureRandomFactory @Inject constructor() {
    fun newInstance() = SecureRandom()
}
