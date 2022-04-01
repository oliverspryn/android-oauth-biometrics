package com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth

import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationRequestBuilderForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.AuthorizationServiceConfigurationForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.UriForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildConfigModule
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.oauth.AuthorizationServiceCouldNotBeConfigured
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import io.reactivex.rxjava3.core.Single
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Named

class InitializeOAuthLoginFlowUseCase @Inject constructor(
    private val authorizationRequestBuilderForwarder: AuthorizationRequestBuilderForwarder,
    private val authorizationServiceConfigurationForwarder: AuthorizationServiceConfigurationForwarder,
    private val authStateManager: AuthStateManager,
    @Named(BuildConfigModule.OAUTH_CLIENT_ID) private val clientId: String,
    @Named(BuildConfigModule.OPENID_CONFIG_URL) private val openIdConfigUrl: String,
    @Named(BuildConfigModule.OAUTH_LOGIN_REDIRECT_URI) private val redirectUri: String,
    private val uriForwarder: UriForwarder
) {

    operator fun invoke(): Single<AuthorizationRequest> = configAuthService()
        .flatMap { callChain ->
            Single.just(buildRequest(callChain))
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

                authStateManager.setAuthServiceConfiguration(serviceConfiguration)
                emitter.onSuccess(serviceConfiguration)
            }
        }

    private fun buildRequest(
        serviceConfiguration: AuthorizationServiceConfiguration
    ) = authorizationRequestBuilderForwarder
        .build(
            configuration = serviceConfiguration,
            clientId = clientId,
            responseType = ResponseTypeValues.CODE,
            redirectUri = uriForwarder.parse(redirectUri)
        )
        .setScopes(
            "offline_access", // Required for refresh token: https://auth0.com/docs/secure/tokens/refresh-tokens/get-refresh-tokens
            "openid",         // Used to get user's profile
            "profile",        // info from Auth0
            "email"           // Per: https://auth0.com/docs/api/authentication#user-profile
        )
        .build()
}
