package com.oliverspryn.android.oauthbiometrics.di.forwarders

import javax.crypto.Cipher
import javax.inject.Inject

class CipherForwarder @Inject constructor() {
    fun getInstance(transformation: String): Cipher = Cipher.getInstance(transformation)
}
