package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.activity.ComponentActivity
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationExceptionForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationResponseForwarder
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.TokenExchangeThrowable
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import javax.inject.Inject

class TokenExchangeUseCase @Inject constructor(
    private val authExceptionForwarder: AuthorizationExceptionForwarder,
    private val authResponseForwarder: AuthorizationResponseForwarder,
    private val authService: AuthorizationService,
    private val authStateManager: AuthStateManager
) {

    operator fun invoke(
        activity: ComponentActivity,
        notLoggingIn: () -> Unit,
        loginSuccess: () -> Unit,
        loginError: (TokenExchangeThrowable) -> Unit
    ) {
        val authCodeResponse = authResponseForwarder.fromIntent(activity.intent)
        val authCodeException = authExceptionForwarder.fromIntent(activity.intent)

        if (authCodeException != null) {
            loginError(TokenExchangeThrowable.AuthorizationCodeError(authCodeException))
            activity.clearIntent()
            return
        }

        if (authCodeResponse == null) {
            notLoggingIn()
            return
        }

        authStateManager.provideAuthorizationCode(authCodeResponse, authCodeException)

        authService.performTokenRequest(
            authCodeResponse.createTokenExchangeRequest()
        ) { tokenResponse, tokenException ->
            if (tokenException != null) {
                loginError(TokenExchangeThrowable.TokenExchangeError(tokenException))
                activity.clearIntent()
                return@performTokenRequest
            }

            if (tokenResponse == null) {
                loginError(TokenExchangeThrowable.NoToken)
                activity.clearIntent()
                return@performTokenRequest
            }

            authStateManager.provideTokens(tokenResponse, tokenException)
            loginSuccess()
            activity.clearIntent()
        }
    }

    private fun ComponentActivity.clearIntent() {
        intent.removeExtra(AuthorizationException.EXTRA_EXCEPTION)
        intent.removeExtra(AuthorizationResponse.EXTRA_RESPONSE)
    }
}
