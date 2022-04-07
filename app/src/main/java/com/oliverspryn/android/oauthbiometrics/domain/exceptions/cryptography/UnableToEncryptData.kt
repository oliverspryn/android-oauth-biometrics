package com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography

class UnableToEncryptData(cause: Throwable) : Throwable(
    message = "The security on the device has changed and this key is no longer able to encrypt any data.",
    cause = cause
)
