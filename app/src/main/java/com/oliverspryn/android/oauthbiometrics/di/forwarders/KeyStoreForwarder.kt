package com.oliverspryn.android.oauthbiometrics.di.forwarders

import java.security.KeyStore
import javax.inject.Inject

class KeyStoreForwarder @Inject constructor() {
    fun getInstance(type: String): KeyStore = KeyStore.getInstance(type)
}
