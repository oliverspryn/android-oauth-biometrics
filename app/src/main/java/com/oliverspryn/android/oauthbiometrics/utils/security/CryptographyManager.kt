package com.oliverspryn.android.oauthbiometrics.utils.security

import android.security.keystore.KeyProperties
import android.util.Base64
import com.oliverspryn.android.oauthbiometrics.di.factories.ByteArrayFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.IvParameterSpecFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.KeyGenParameterSpecBuilderFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.SecureRandomFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.StringFactory
import com.oliverspryn.android.oauthbiometrics.di.forwarders.CipherForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.KeyGeneratorForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.KeyStoreForwarder
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.BYTE_ARRAY_ENCODING
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Base64.DELIMITER
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Base64.ENCODING
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Encryption.ALGORITHM
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Encryption.BLOCK_MODE
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Encryption.PADDING
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.KEY_STORE_NAME
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Key.NAME
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Key.SIZE
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject

class CryptographyManager @Inject constructor(
    private val byteArrayFactory: ByteArrayFactory,
    private val cipherForwarder: CipherForwarder,
    private val ivParameterSpecFactory: IvParameterSpecFactory,
    private val keyGeneratorForwarder: KeyGeneratorForwarder,
    private val keyGenParameterSpecBuilderFactory: KeyGenParameterSpecBuilderFactory,
    private val keyStoreForwarder: KeyStoreForwarder,
    private val secureRandomFactory: SecureRandomFactory,
    private val stringFactory: StringFactory
) {

    fun decryptData(cipherText: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(cipherText)
        return stringFactory.newInstance(plaintext, BYTE_ARRAY_ENCODING)
    }

    fun encryptData(plaintext: String, cipher: Cipher): EncryptedData {
        val cipherText = cipher.doFinal(plaintext.toByteArray(BYTE_ARRAY_ENCODING))

        return EncryptedData(
            cipherText = cipherText,
            iv = cipher.iv
        )
    }

    fun getInitializedCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            ivParameterSpecFactory.newInstance(iv)
        )

        return cipher
    }

    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    private fun createIv(cipherBlockSize: Int): IvParameterSpec {
        val random = secureRandomFactory.newInstance()
        val iv = byteArrayFactory.newInstance(cipherBlockSize)

        random.nextBytes(iv)
        return ivParameterSpecFactory.newInstance(iv)
    }

    private fun createSecretKey(): Key {
        val keyGeneratorParametersBuilder = keyGenParameterSpecBuilderFactory.build(
            keystoreAlias = NAME,
            purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )

        keyGeneratorParametersBuilder.setBlockModes(BLOCK_MODE)
        keyGeneratorParametersBuilder.setEncryptionPaddings(PADDING)
        keyGeneratorParametersBuilder.setKeySize(SIZE)
        keyGeneratorParametersBuilder.setUserAuthenticationRequired(true)

        val keyGeneratorParameters = keyGeneratorParametersBuilder.build()
        val keyGenerator = keyGeneratorForwarder.getInstance(ALGORITHM, KEY_STORE_NAME)
        keyGenerator.init(keyGeneratorParameters)

        return keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        val transformation = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        return cipherForwarder.getInstance(transformation)
    }

    private fun getOrCreateSecretKey(): Key {
        val keyStore = keyStoreForwarder.getInstance(KEY_STORE_NAME)
        keyStore.load(null) // Keystore must be loaded before it can be accessed

        return keyStore.getKey(NAME, null) ?: createSecretKey()
    }
}

data class EncryptedData(
    val cipherText: ByteArray,
    val iv: ByteArray
) {
    companion object {
        fun fromString(encodedString: String): EncryptedData? {
            val parts = encodedString.split(DELIMITER)
            if (parts.size != 2) return null

            val cipherText = Base64.decode(parts[0], ENCODING)
            val iv = Base64.decode(parts[1], ENCODING)

            return EncryptedData(
                cipherText = cipherText,
                iv = iv
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!cipherText.contentEquals(other.cipherText)) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cipherText.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }

    override fun toString(): String {
        val base64CipherText = Base64.encodeToString(cipherText, ENCODING)
        val base64IV = Base64.encodeToString(iv, ENCODING)

        return "$base64CipherText$DELIMITER$base64IV"
    }
}
