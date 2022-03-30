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

    var serializedAuthState: String?
        get() = authState?.jsonSerializeString()
        set(value) {
            authState = if (value != null) {
                AuthState.jsonDeserialize(value)
            } else {
                AuthState()
            }
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

    fun setAuthServiceConfiguration(serviceConfiguration: AuthorizationServiceConfiguration) {
        authState = AuthState(serviceConfiguration)
    }
}
