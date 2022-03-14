package com.oliverspryn.android.oauthbiometrics.di.modules

import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
class RxJavaModule {

    @Provides
    fun provideRxJavaFactory() = RxJavaFactory()
}
