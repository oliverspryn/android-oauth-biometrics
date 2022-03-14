package com.oliverspryn.android.oauthbiometrics.di.factories

import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import javax.inject.Inject

class AuthStateFactory @Inject constructor() {
    fun newInstance(
        serviceConfiguration: AuthorizationServiceConfiguration
    ) = AuthState(
        serviceConfiguration
    )
}
