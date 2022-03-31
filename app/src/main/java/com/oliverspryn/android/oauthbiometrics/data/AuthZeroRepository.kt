package com.oliverspryn.android.oauthbiometrics.data

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface AuthZeroRepository {

    data class UserInfo(
        val email: String?,
        val name: String?,
        val nickName: String?,
        val picture: String?
    )

    @GET("userinfo")
    fun getUserInfo(): Single<UserInfo>
}
