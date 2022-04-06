package com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography

import javax.crypto.IllegalBlockSizeException

class UnableToDecryptData(cause: IllegalBlockSizeException) : Throwable(
    message = "The security on the device has changed and this key is no longer able to decrypt any data.",
    cause = cause
)
