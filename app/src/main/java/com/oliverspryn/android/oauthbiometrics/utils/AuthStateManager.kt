package com.oliverspryn.android.oauthbiometrics.utils

import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenResponse
import javax.inject.Inject

class AuthStateManager @Inject constructor() {

    private companion object {
        private var authState: AuthState? = null
    }

    fun getSerializedAuthState(): String? = authState?.jsonSerializeString()

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

    fun setAuthServiceConfiguration(serviceConfiguration: AuthorizationServiceConfiguration) {
        authState = AuthState(serviceConfiguration)
    }
}
