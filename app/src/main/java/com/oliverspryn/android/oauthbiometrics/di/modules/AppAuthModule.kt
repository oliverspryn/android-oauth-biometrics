package com.oliverspryn.android.oauthbiometrics.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.openid.appauth.AuthorizationService

@Module
@InstallIn(SingletonComponent::class)
class AppAuthModule {

    @Provides
    fun provideAuthorizationService(
        @ApplicationContext context: Context
    ) = AuthorizationService(context)
}
