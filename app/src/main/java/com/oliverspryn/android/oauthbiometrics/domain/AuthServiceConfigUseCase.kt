package com.oliverspryn.android.oauthbiometrics.domain

import net.openid.appauth.AuthorizationServiceConfiguration

class AuthServiceConfigUseCase {

    operator fun invoke() {
        val serviceConfig = AuthorizationServiceConfiguration(

        )
    }
}