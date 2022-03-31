package com.oliverspryn.android.oauthbiometrics.domain.exceptions

object NoAccessTokenProvidedInResponse :
    Throwable("The IDP did not respond with a usable access token when requesting a new one.")
