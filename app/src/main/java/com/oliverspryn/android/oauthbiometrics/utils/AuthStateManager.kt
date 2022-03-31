package com.oliverspryn.android.oauthbiometrics.utils

import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenResponse
import javax.inject.Inject

class AuthStateManager @Inject constructor() {

    private companion object {
        private var cachedAuthState: AuthState? = null
    }

    val authServiceConfiguration: AuthorizationServiceConfiguration?
        get() = cachedAuthState?.authorizationServiceConfiguration

    val idToken: String?
        get() = cachedAuthState?.idToken

    var serializedAuthState: String?
        get() = cachedAuthState?.jsonSerializeString()
        set(value) {
            cachedAuthState = if (value != null) {
                AuthState.jsonDeserialize(value)
            } else {
                AuthState()
            }
        }

    fun clearAuthState() {
        cachedAuthState = null
    }

    fun performActionWithFreshTokens(
        service: AuthorizationService,
        action: AuthState.AuthStateAction
    ) {
        val localAuthState = cachedAuthState ?: return action.execute(
            null,
            null,
            AuthorizationException(
                AuthorizationException.TYPE_GENERAL_ERROR,
                100, // Docs recommend error codes between 0-999 for this class. 0-9 are used by the library.
                null,
                "The application does not have an authentication state for this user.",
                null,
                null
            )
        )

        localAuthState.performActionWithFreshTokens(service, action)
    }

    fun provideAuthorizationCode(
        response: AuthorizationResponse,
        exception: AuthorizationException?
    ) {
        cachedAuthState?.update(response, exception)
    }

    fun provideTokens(
        response: TokenResponse,
        exception: AuthorizationException?
    ) {
        cachedAuthState?.update(response, exception)
    }

    fun setAuthServiceConfiguration(serviceConfiguration: AuthorizationServiceConfiguration) {
        cachedAuthState = AuthState(serviceConfiguration)
    }
}
