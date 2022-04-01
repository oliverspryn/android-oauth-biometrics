package com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth

import androidx.fragment.app.FragmentActivity
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationExceptionForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationResponseForwarder
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import io.reactivex.rxjava3.core.Single
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
        activity: FragmentActivity
    ): Single<TokenExchangeOutcome> = Single.create { singleEmitter ->
        val authCodeResponse = authResponseForwarder.fromIntent(activity.intent)
        val authCodeException = authExceptionForwarder.fromIntent(activity.intent)

        if (authCodeException != null) {
            singleEmitter.onError(
                TokenExchangeOutcome.Error.AuthorizationCodeError(
                    authCodeException
                )
            )

            activity.clearIntent()
            return@create
        }

        if (authCodeResponse == null) {
            singleEmitter.onSuccess(TokenExchangeOutcome.NotLoggingIn)
            return@create
        }

        authStateManager.provideAuthorizationCode(authCodeResponse, authCodeException)

        authService.performTokenRequest(
            authCodeResponse.createTokenExchangeRequest()
        ) { tokenResponse, tokenException ->
            if (tokenException != null) {
                singleEmitter.onError(TokenExchangeOutcome.Error.TokenExchangeError(tokenException))
                activity.clearIntent()
                return@performTokenRequest
            }

            if (tokenResponse == null) {
                singleEmitter.onError(TokenExchangeOutcome.Error.NoToken)
                activity.clearIntent()
                return@performTokenRequest
            }

            authStateManager.provideTokens(tokenResponse, tokenException)
            singleEmitter.onSuccess(TokenExchangeOutcome.LoginSuccess)
            activity.clearIntent()
        }
    }
}

sealed interface TokenExchangeOutcome {
    object LoginSuccess : TokenExchangeOutcome
    object NotLoggingIn : TokenExchangeOutcome

    sealed class Error(
        message: String,
        cause: Throwable?
    ) : Throwable(message, cause), TokenExchangeOutcome {

        class AuthorizationCodeError(cause: Throwable) : Error(
            message = "Client encountered an error while obtaining the authorization code from the provider",
            cause = cause
        )

        object NoToken : Error(
            message = "No was given from the provider",
            cause = null
        )

        class TokenExchangeError(cause: Throwable) : Error(
            message = "Client encountered an error while exchanging the authorization code for the tokens",
            cause = cause
        )
    }
}

private fun FragmentActivity.clearIntent() {
    intent.removeExtra(AuthorizationException.EXTRA_EXCEPTION)
    intent.removeExtra(AuthorizationResponse.EXTRA_RESPONSE)
}
