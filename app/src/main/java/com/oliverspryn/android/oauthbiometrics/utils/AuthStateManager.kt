package com.oliverspryn.android.oauthbiometrics.utils

import net.openid.appauth.*
import java.lang.Exception
import javax.inject.Inject

class AuthStateManager @Inject constructor() {

    private companion object {
        private var authState: AuthState? = null
    }

    fun setAuthServiceConfiguration(serviceConfiguration: AuthorizationServiceConfiguration) {
        authState = AuthState(serviceConfiguration)
    }

    fun provideAuthorizationCode(
        response: AuthorizationResponse,
        exception: AuthorizationException?
    ) {
        authState?.update(response, exception)
    }

    fun provideTokens(
        response: TokenResponse,
        exception: AuthorizationException?
    ) {
        authState?.update(response, exception)
    }
}
