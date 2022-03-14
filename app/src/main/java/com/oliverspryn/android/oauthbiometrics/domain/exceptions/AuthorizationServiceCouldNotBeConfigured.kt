package com.oliverspryn.android.oauthbiometrics.domain.exceptions

object AuthorizationServiceCouldNotBeConfigured :
    Throwable("The authorization service could not be created because a call to the OpenID endpoint failed.")
