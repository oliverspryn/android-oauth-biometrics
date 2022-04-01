package com.oliverspryn.android.oauthbiometrics.domain.usecases.storage

import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.DidHandleLogoutWithOAuthRedirect
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.LaunchOAuthLogoutFlowUseCase
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authStateManager: AuthStateManager,
    private val deletePersistentAuthStateUseCase: DeletePersistentAuthStateUseCase,
    private val launchOAuthLogoutFlowUseCase: LaunchOAuthLogoutFlowUseCase
) {

    operator fun invoke(doOAuthLogout: Boolean = true): Single<DidHandleLogoutWithOAuthRedirect> = Single
        .create<DidHandleLogoutWithOAuthRedirect> { singleEmitter ->
            if (doOAuthLogout) {
                singleEmitter.onSuccess(launchOAuthLogoutFlowUseCase())
            } else {
                singleEmitter.onSuccess(DidHandleLogoutWithOAuthRedirect.No)
            }
        }
        .flatMap { didHandleLogoutRedirect ->
            authStateManager.clearAuthState()
            Single.just(didHandleLogoutRedirect)
        }
        .flatMap { didHandleLogoutRedirect ->
            deletePersistentAuthStateUseCase()
                .andThen(Single.just(didHandleLogoutRedirect))
        }
}
