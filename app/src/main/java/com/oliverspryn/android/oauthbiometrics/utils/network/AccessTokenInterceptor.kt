package com.oliverspryn.android.oauthbiometrics.utils.network

import android.content.Context
import android.content.Intent
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.GetFreshAccessTokenUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.LogoutUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AccessTokenInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFreshAccessTokenUseCase: GetFreshAccessTokenUseCase,
    private val intentFactory: IntentFactory,
    private val logoutUseCase: LogoutUseCase,
    private val rxJavaFactory: RxJavaFactory
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
        val restartActivity = intentFactory
            .newInstance(context, MainActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }

        // Network operation already on background thread
        // Jump to UI for navigation
        logoutUseCase(false)
            .observeOn(rxJavaFactory.ui)
            .subscribe({
                // Did not use the OAuth logout, since the token was already shot
                context.startActivity(restartActivity)
            }, {
                context.startActivity(restartActivity)
            })
    }
}

private fun Int.isUnauthorized(): Boolean = this == 401
