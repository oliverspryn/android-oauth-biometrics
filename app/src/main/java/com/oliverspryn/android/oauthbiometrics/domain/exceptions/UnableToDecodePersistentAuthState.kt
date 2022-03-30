package com.oliverspryn.android.oauthbiometrics.domain.exceptions

object UnableToDecodePersistentAuthState :
    IllegalStateException("The serialized auth state persisted on disk cannot be decoded into a usable format.")
