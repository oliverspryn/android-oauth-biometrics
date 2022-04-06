package com.oliverspryn.android.oauthbiometrics.domain.exceptions.storage

object UnableToDecodeVolatileAuthState :
    IllegalStateException("The auth state in volatile memory cannot be serialized into a usable format.")
