package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.content.Intent
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

class AuthorizationResponseForwarder @Inject constructor() {

    fun fromIntent(dataIntent: Intent): AuthorizationResponse? =
        AuthorizationResponse.fromIntent(dataIntent)
}
