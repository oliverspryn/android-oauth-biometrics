package com.oliverspryn.android.oauthbiometrics.domain.usecases

import com.oliverspryn.android.oauthbiometrics.domain.exceptions.NoAccessTokenProvidedInResponse
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import io.reactivex.rxjava3.core.Single
import net.openid.appauth.AuthorizationService
import javax.inject.Inject

class GetFreshAccessTokenUseCase @Inject constructor(
    private val authorizationService: AuthorizationService,
    private val authStateManager: AuthStateManager
) {

    operator fun invoke(): Single<String> = Single.create { singleEmitter ->
        authStateManager.performActionWithFreshTokens(authorizationService) { accessToken, _, exception ->
            if (exception != null) {
                singleEmitter.onError(exception)
            } else if (accessToken.isNullOrBlank()) {
                singleEmitter.onError(NoAccessTokenProvidedInResponse)
            } else {
                singleEmitter.onSuccess(accessToken)
            }
        }
    }
}
