package com.oliverspryn.android.oauthbiometrics.di.modules

import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
class BuildModule {

    companion object {
        const val SDK_INT = "com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule.SDK_INT"
    }

    @Provides
    @Named(SDK_INT)
    fun provideSdkInt() = Build.VERSION.SDK_INT
}