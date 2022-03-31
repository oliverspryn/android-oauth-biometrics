package com.oliverspryn.android.oauthbiometrics.domain.usecases

import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authStateManager: AuthStateManager,
    private val deletePersistentAuthStateUseCase: DeletePersistentAuthStateUseCase,
    private val launchOAuthLogoutFlowUseCase: LaunchOAuthLogoutFlowUseCase
) {

    operator fun invoke(): DidHandleLogoutRedirect {
        val outcome = launchOAuthLogoutFlowUseCase()

        authStateManager.clearAuthState()
        deletePersistentAuthStateUseCase(
            onComplete = {},
            onError = {}
        )

        return outcome
    }
}
