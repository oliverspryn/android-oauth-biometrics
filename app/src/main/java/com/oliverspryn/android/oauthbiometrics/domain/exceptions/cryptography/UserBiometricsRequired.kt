package com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography

class UserBiometricsRequired(cause: IllegalStateException) : Throwable(
    message = "The Android key generator requires the device owner to register at least biometric entity, such as a fingerprint, iris, or face.",
    cause = cause
)
