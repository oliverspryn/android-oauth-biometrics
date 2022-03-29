package com.oliverspryn.android.oauthbiometrics.domain.exceptions

import android.security.keystore.KeyPermanentlyInvalidatedException

class UnableToInitializeCipher(cause: KeyPermanentlyInvalidatedException) : Throwable(
    message = "The key has been rotated within the Android keystore and any ciphertext encrypted with the old key is no longer accessible.",
    cause = cause
)
