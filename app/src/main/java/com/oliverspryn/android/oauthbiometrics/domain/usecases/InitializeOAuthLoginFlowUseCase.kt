package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.di.factories.AuthStateFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.AuthorizationServiceFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationRequestBuilderForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationServiceConfigurationForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.PendingIntentForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.UriForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildConfigModule
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.AuthorizationServiceCouldNotBeConfigured
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Named

class InitializeOAuthLoginFlowUseCase @Inject constructor(
    private val authorizationRequestBuilderForwarder: AuthorizationRequestBuilderForwarder,
    private val authorizationServiceConfigurationForwarder: AuthorizationServiceConfigurationForwarder,
    private val authServiceFactory: AuthorizationServiceFactory,
    private val authStateFactory: AuthStateFactory,
    @Named(BuildConfigModule.OAUTH_CLIENT_ID) private val clientId: String,
    @ApplicationContext private val context: Context,
    private val intentFactory: IntentFactory,
    @Named(BuildConfigModule.OPENID_CONFIG_URL) private val openIdConfigUrl: String,
    private val pendingIntentForwarder: PendingIntentForwarder,
    @Named(BuildConfigModule.OAUTH_REDIRECT_URI) private val redirectUri: String,
    private val rxJavaFactory: RxJavaFactory,
    @Named(BuildModule.SDK_INT) private val sdkInt: Int,
    private val uriForwarder: UriForwarder
) {

    companion object {
        const val REQUEST_CODE = 14529
    }

    operator fun invoke(): Completable = Completable
        .complete()
        .observeOn(rxJavaFactory.io)
        .andThen(configAuthService())
        .flatMap { serviceConfig ->
            Single.just(buildRequestAndAuthState(serviceConfig))
        }
        .flatMapCompletable { callChain ->
            openBrowserForLogin(callChain)
            Completable.complete()
        }

    private fun buildRequestAndAuthState(
        serviceConfig: AuthorizationServiceConfiguration
    ): CallChain {
        val request = authorizationRequestBuilderForwarder
            .build(
                configuration = serviceConfig,
                clientId = clientId,
                responseType = ResponseTypeValues.CODE,
                redirectUri = uriForwarder.parse(redirectUri)
            )
            .build()

        return CallChain(
            authRequest = request,
            authState = authStateFactory.newInstance(serviceConfig)
        )
    }

    private fun configAuthService(): Single<AuthorizationServiceConfiguration> = Single
        .create { emitter ->
            authorizationServiceConfigurationForwarder.fetchFromIssuer(
                uriForwarder.parse(openIdConfigUrl)
            ) { serviceConfiguration, ex ->
                if (ex != null) {
                    emitter.onError(ex)
                    return@fetchFromIssuer
                }

                if (serviceConfiguration == null) {
                    emitter.onError(AuthorizationServiceCouldNotBeConfigured)
                    return@fetchFromIssuer
                }

                emitter.onSuccess(serviceConfiguration)
            }
        }

    @SuppressLint("InlinedApi") // Lint tool doesn't know I checked properly
    private fun openBrowserForLogin(callChain: CallChain) {
        var flags = 0

        // Per: https://github.com/openid/AppAuth-Android/issues/746
        if (sdkInt >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE
        }

        val authService = authServiceFactory.newInstance(context)
        val pendingIntent = pendingIntentForwarder.getActivity(
            context,
            REQUEST_CODE,
            intentFactory.newInstance(context, MainActivity::class.java),
            flags
        )

        authService.performAuthorizationRequest(
            callChain.authRequest,
            pendingIntent,
            pendingIntent
        )
    }
}

data class CallChain(
    val authRequest: AuthorizationRequest,
    val authState: AuthState
)
