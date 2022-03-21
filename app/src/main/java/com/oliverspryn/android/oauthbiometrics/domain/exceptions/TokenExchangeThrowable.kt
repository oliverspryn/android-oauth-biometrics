package com.oliverspryn.android.oauthbiometrics.domain.exceptions

sealed class TokenExchangeThrowable(
    message: String,
    cause: Throwable?
) : Throwable(message, cause) {

    class AuthorizationCodeError(cause: Throwable) :
        TokenExchangeThrowable(
            message = "Client encountered an error while obtaining the authorization code from the provider",
            cause = cause
        )

    object NoToken :
        TokenExchangeThrowable(
            message = "No was given from the provider",
            cause = null
        )

    class TokenExchangeError(cause: Throwable) :
        TokenExchangeThrowable(
            message = "Client encountered an error while exchanging the authorization code for the tokens",
            cause = cause
        )
}
