package com.oliverspryn.android.oauthbiometrics.utils.network

import android.content.Context
import android.content.Intent
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.DeletePersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.GetFreshAccessTokenUseCase
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AccessTokenInterceptor @Inject constructor(
    private val authStateManager: AuthStateManager,
    @ApplicationContext private val context: Context,
    private val deletePersistentAuthStateUseCase: DeletePersistentAuthStateUseCase,
    private val getFreshAccessTokenUseCase: GetFreshAccessTokenUseCase,
    private val intentFactory: IntentFactory
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var accessToken = ""
        var encounteredError = false

        // Network operations must happen on a background thread per Android's rules
        // So, blocking a background thread while waiting for a response is just fine
        getFreshAccessTokenUseCase()
            .blockingSubscribe({
                accessToken = it
            }, {
                encounteredError = true
                handleError()
            })

        // Don't modify the request/response, already encountered a problem
        if (encounteredError) {
            val originalRequest = chain.request()
            return chain.proceed(originalRequest)
        }

        val newRequest = chain
            .request()
            .newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(newRequest)

        // Refreshing access token didn't work :'(
        if (response.code.isUnauthorized()) {
            handleError()
        }

        return response
    }

    private fun handleError() {
        authStateManager.clearAuthState()
        deletePersistentAuthStateUseCase(
            onComplete = { },
            onError = { /* Oh well, we tried */ }
        )

        val restartActivity = intentFactory
            .newInstance(context, MainActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }

        context.startActivity(restartActivity)
    }
}

private fun Int.isUnauthorized(): Boolean = this == 401
