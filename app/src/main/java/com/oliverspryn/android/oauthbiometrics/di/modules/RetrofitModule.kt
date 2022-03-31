package com.oliverspryn.android.oauthbiometrics.di.modules

import com.oliverspryn.android.oauthbiometrics.BuildConfig
import com.oliverspryn.android.oauthbiometrics.data.AuthZeroRepository
import com.oliverspryn.android.oauthbiometrics.utils.network.AccessTokenInterceptor
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(ViewModelComponent::class)
class RetrofitModule {

    @Provides
    @Reusable
    fun provideAuthZeroRepository(
        accessTokenInterceptor: AccessTokenInterceptor
    ): AuthZeroRepository = Retrofit
        .Builder()
        .baseUrl(BuildConfig.OPENID_CONFIG_URL)
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient
                .Builder()
                .addInterceptor(accessTokenInterceptor)
                .build()
        )
        .build()
        .create(AuthZeroRepository::class.java)
}
