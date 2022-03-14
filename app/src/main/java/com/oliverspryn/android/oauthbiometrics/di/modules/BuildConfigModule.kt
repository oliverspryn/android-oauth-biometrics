package com.oliverspryn.android.oauthbiometrics.di.modules

import com.oliverspryn.android.oauthbiometrics.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
class BuildConfigModule {

    companion object {
        const val OAUTH_CLIENT_ID =
            "com.oliverspryn.android.oauthbiometrics.di.modules.BuildConfigModule.OAUTH_CLIENT_ID"

        const val OAUTH_REDIRECT_URI =
            "com.oliverspryn.android.oauthbiometrics.di.modules.BuildConfigModule.OAUTH_REDIRECT_URI"

        const val OPENID_CONFIG_URL =
            "com.oliverspryn.android.oauthbiometrics.di.modules.BuildConfigModule.OPENID_CONFIG_URL"
    }

    @Provides
    @Named(OAUTH_CLIENT_ID)
    fun provideOAuthClientId(): String = BuildConfig.OAUTH_CLIENT_ID

    @Provides
    @Named(OAUTH_REDIRECT_URI)
    fun provideOAuthRedirectUri(): String = BuildConfig.OAUTH_REDIRECT_URI

    @Provides
    @Named(OPENID_CONFIG_URL)
    fun provideOpenIdConfigUrl(): String = BuildConfig.OPENID_CONFIG_URL
}