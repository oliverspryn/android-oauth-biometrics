package com.oliverspryn.android.oauthbiometrics.di.factories

import android.content.Context
import net.openid.appauth.AuthorizationService
import javax.inject.Inject

class AuthorizationServiceFactory @Inject constructor() {
    fun newInstance(context: Context) = AuthorizationService(context)
}
