package com.oliverspryn.android.oauthbiometrics.di.factories

import java.nio.charset.Charset
import javax.inject.Inject

class StringFactory @Inject constructor() {
    fun newInstance(
        bytes: ByteArray,
        charset: Charset
    ) = String(
        bytes = bytes,
        charset = charset
    )
}
