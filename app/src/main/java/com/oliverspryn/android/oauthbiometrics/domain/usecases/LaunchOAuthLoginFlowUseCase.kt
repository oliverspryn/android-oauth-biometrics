package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.forwarders.PendingIntentForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import javax.inject.Inject
import javax.inject.Named

class LaunchOAuthLoginFlowUseCase @Inject constructor(
    private val authService: AuthorizationService,
    @ApplicationContext private val context: Context,
    private val intentFactory: IntentFactory,
    private val pendingIntentForwarder: PendingIntentForwarder,
    @Named(BuildModule.SDK_INT) private val sdkInt: Int
) {

    companion object {
        const val LOGIN_FAILED =
            "com.oliverspryn.android.oauthbiometrics.domain.usecases.LaunchOAuthLoginFlowUseCase.LOGIN_FAILED"

        const val REQUEST_CODE = 14529
    }

    operator fun invoke(request: AuthorizationRequest) {
        openBrowserForLogin(request)
    }

    @SuppressLint("InlinedApi") // Lint tool doesn't know I checked properly
    private fun openBrowserForLogin(request: AuthorizationRequest) {
        var flags = 0

        // Per: https://github.com/openid/AppAuth-Android/issues/746
        if (sdkInt >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE
        }

        val canceledIntent = intentFactory.newInstance(context, MainActivity::class.java)
        val completedIntent = intentFactory.newInstance(context, MainActivity::class.java)
        canceledIntent.putExtra(LOGIN_FAILED, true)
        canceledIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val canceledPendingIntent = pendingIntentForwarder.getActivity(
            context,
            REQUEST_CODE,
            canceledIntent,
            flags
        )

        val completedPendingIntent = pendingIntentForwarder.getActivity(
            context,
            REQUEST_CODE,
            completedIntent,
            flags
        )

        authService.performAuthorizationRequest(
            request,
            completedPendingIntent,
            canceledPendingIntent
        )
    }
}
