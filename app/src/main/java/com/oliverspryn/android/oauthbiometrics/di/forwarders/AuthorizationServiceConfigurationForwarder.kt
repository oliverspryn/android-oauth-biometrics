package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.net.Uri
import net.openid.appauth.AuthorizationServiceConfiguration
import javax.inject.Inject

class AuthorizationServiceConfigurationForwarder @Inject constructor() {
    fun fetchFromIssuer(
        openIdConnectIssuerUri: Uri,
        callback: AuthorizationServiceConfiguration.RetrieveConfigurationCallback
    ) = AuthorizationServiceConfiguration.fetchFromIssuer(
        openIdConnectIssuerUri,
        callback
    )
}