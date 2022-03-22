package com.oliverspryn.android.oauthbiometrics.utils.security

import android.security.keystore.KeyProperties
import java.nio.charset.Charset

object CryptographyConfig {

    object Base64 {
        const val DELIMITER = "|"
        const val ENCODING = android.util.Base64.DEFAULT
    }

    object Encryption {
        const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    }

    object Key {
        const val NAME =
            "com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Key.NAME"

        const val SIZE = 256
    }

    val BYTE_ARRAY_ENCODING: Charset = Charset.forName("UTF-8")
    const val KEY_STORE_NAME =
        "com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.KEY_STORE_NAME"
}
