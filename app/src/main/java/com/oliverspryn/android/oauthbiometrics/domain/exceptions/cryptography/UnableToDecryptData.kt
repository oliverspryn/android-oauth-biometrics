package com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography

class UnableToDecryptData(cause: Throwable) : Throwable(
    message = "The security on the device has changed and this key is no longer able to decrypt any data.",
    cause = cause
)
