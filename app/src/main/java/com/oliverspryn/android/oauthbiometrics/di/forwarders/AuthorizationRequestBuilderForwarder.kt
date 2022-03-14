package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import javax.inject.Inject

class AuthorizationRequestBuilderForwarder @Inject constructor() {

    fun build(
        configuration: AuthorizationServiceConfiguration,
        clientId: String,
        responseType: String,
        redirectUri: Uri
    ) = AuthorizationRequest.Builder(
        configuration,
        clientId,
        responseType,
        redirectUri
    )
}
