package com.oliverspryn.android.oauthbiometrics.utils.security

import android.security.keystore.KeyProperties

object CryptographyConfig {

    object Base64 {
        const val DELIMITER = "|"
        const val ENCODING = android.util.Base64.DEFAULT
    }

    object Encryption {
        const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    }

    object Key {
        const val NAME =
            "com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Key.NAME"

        const val SIZE = 256
    }

    // This framework can be configured to allow API 30+ devices to use
    // the device credentials (PIN/pattern/etc) either in place of, or
    // in addition to the biometric offering to secure the AES key.
    //
    // However, enabling this flag comes with an apparent trade off. To
    // maximize user security, this framework configures the AES key to
    // become invalidated any time the user adds another biometric option
    // such as an additional fingerprint. This protects the user's
    // information from malicious actors who gain access to their device
    // and register their own biometric data. Android does not appear to
    // do this when a PIN/pattern is available, even if that credential
    // is changed. It does, however, do this key rotation when the PIN
    // is removed completely and re-added later.
    //
    // By disabling this flag, the users must authenticate with his or her
    // biometrics and run the risk of not being able to use this app's
    // secondary authentication if the biometrics fail too many times.
    const val ALLOW_DEVICE_CREDENTIALS_AS_SECONDARY_LOGIN = true
    val BYTE_ARRAY_ENCODING = Charsets.UTF_8
    const val KEY_STORE_NAME = "AndroidKeyStore"
}
