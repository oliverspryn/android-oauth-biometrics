package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.content.Intent
import net.openid.appauth.AuthorizationException
import javax.inject.Inject

class AuthorizationExceptionForwarder @Inject constructor() {

    fun fromIntent(data: Intent): AuthorizationException? =
        AuthorizationException.fromIntent(data)
}
