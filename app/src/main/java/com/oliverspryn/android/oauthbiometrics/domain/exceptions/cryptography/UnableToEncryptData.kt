package com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography

import javax.crypto.IllegalBlockSizeException

class UnableToEncryptData(cause: IllegalBlockSizeException) : Throwable(
    message = "The security on the device has changed and this key is no longer able to encrypt any data.",
    cause = cause
)
