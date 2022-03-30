package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
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
        activity: FragmentActivity,
        onNotLoggingIn: () -> Unit,
        onLoginSuccess: () -> Unit,
        onLoginError: (TokenExchangeThrowable) -> Unit
    ) {
        val authCodeResponse = authResponseForwarder.fromIntent(activity.intent)
        val authCodeException = authExceptionForwarder.fromIntent(activity.intent)

        if (authCodeException != null) {
            onLoginError(TokenExchangeThrowable.AuthorizationCodeError(authCodeException))
            activity.clearIntent()
            return
        }

        if (authCodeResponse == null) {
            onNotLoggingIn()
            return
        }

        authStateManager.provideAuthorizationCode(authCodeResponse, authCodeException)

        authService.performTokenRequest(
            authCodeResponse.createTokenExchangeRequest()
        ) { tokenResponse, tokenException ->
            if (tokenException != null) {
                onLoginError(TokenExchangeThrowable.TokenExchangeError(tokenException))
                activity.clearIntent()
                return@performTokenRequest
            }

            if (tokenResponse == null) {
                onLoginError(TokenExchangeThrowable.NoToken)
                activity.clearIntent()
                return@performTokenRequest
            }

            authStateManager.provideTokens(tokenResponse, tokenException)
            onLoginSuccess()
            activity.clearIntent()
        }
    }

    private fun ComponentActivity.clearIntent() {
        intent.removeExtra(AuthorizationException.EXTRA_EXCEPTION)
        intent.removeExtra(AuthorizationResponse.EXTRA_RESPONSE)
    }
}
