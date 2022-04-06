package com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography

class UnableToInitializeCipher : Throwable {
    constructor() : super(
        message = "The key has been rotated within the Android keystore and any ciphertext encrypted with the old key is no longer accessible."
    )

    constructor(cause: Throwable) : super(
        message = "The key has been rotated within the Android keystore and any ciphertext encrypted with the old key is no longer accessible.",
        cause = cause
    )
}
