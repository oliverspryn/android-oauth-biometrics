package com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.forwarders.PendingIntentForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.UriForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildConfigModule
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import javax.inject.Inject
import javax.inject.Named

class LaunchOAuthLogoutFlowUseCase @Inject constructor(
    private val authService: AuthorizationService,
    private val authStateManager: AuthStateManager,
    @ApplicationContext private val context: Context,
    private val intentFactory: IntentFactory,
    private val pendingIntentForwarder: PendingIntentForwarder,
    @Named(BuildConfigModule.OAUTH_LOGOUT_REDIRECT_URI) private val redirectUri: String,
    @Named(BuildModule.SDK_INT) private val sdkInt: Int,
    private val uriForwarder: UriForwarder
) {

    companion object {
        const val LOGOUT_FAILED =
            "com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.LaunchOAuthLogoutFlowUseCase.LOGOUT_FAILED"

        const val REQUEST_CODE = 14530
    }

    operator fun invoke(): DidHandleLogoutWithOAuthRedirect {
        val authServiceConfig = authStateManager.authServiceConfiguration
            ?: return DidHandleLogoutWithOAuthRedirect.No

        val idToken = authStateManager.idToken
            ?: return DidHandleLogoutWithOAuthRedirect.No

        val logoutRedirectUri = uriForwarder.parse(redirectUri)

        // IDP did not provide an end session URL
        if (authServiceConfig.endSessionEndpoint == null) {
            return DidHandleLogoutWithOAuthRedirect.No
        }

        val endSessionRequest = EndSessionRequest
            .Builder(authServiceConfig)
            .apply {
                setIdTokenHint(idToken)
                setPostLogoutRedirectUri(logoutRedirectUri)
            }
            .build()

        openBrowserForLogout(endSessionRequest)
        return DidHandleLogoutWithOAuthRedirect.Yes
    }

    @SuppressLint("InlinedApi") // Lint tool doesn't know I checked properly
    private fun openBrowserForLogout(request: EndSessionRequest) {
        var flags = 0

        // Per: https://github.com/openid/AppAuth-Android/issues/746
        if (sdkInt >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE
        }

        val canceledIntent = intentFactory.newInstance(context, MainActivity::class.java)
        val completedIntent = intentFactory.newInstance(context, MainActivity::class.java)
        canceledIntent.putExtra(LOGOUT_FAILED, true)
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

        authService.performEndSessionRequest(
            request,
            completedPendingIntent,
            canceledPendingIntent
        )
    }
}

sealed interface DidHandleLogoutWithOAuthRedirect {
    object No : DidHandleLogoutWithOAuthRedirect
    object Yes : DidHandleLogoutWithOAuthRedirect
}
