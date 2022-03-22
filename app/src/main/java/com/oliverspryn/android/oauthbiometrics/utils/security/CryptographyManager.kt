package com.oliverspryn.android.oauthbiometrics.utils.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject

class CryptographyManager @Inject constructor() {

    fun decryptData(encryptedData: EncryptedData): String {
        val cipher = getInitializedCipherForDecryption(encryptedData.initializationVector)
        val plaintext = cipher.doFinal(encryptedData.cipherText)

        return String(plaintext, BYTE_ARRAY_ENCODING)
    }

    fun encryptData(plaintext: String): EncryptedData {
        val cipher = getInitializedCipherForEncryption()
        val cipherText = cipher.doFinal(plaintext.toByteArray(BYTE_ARRAY_ENCODING))

        return EncryptedData(
            cipherText = cipherText,
            initializationVector = cipher.iv
        )
    }

    private fun createIv(cipherBlockSize: Int): IvParameterSpec {
        val random = SecureRandom()
        val iv = ByteArray(cipherBlockSize)

        random.nextBytes(iv)
        return IvParameterSpec(iv)
    }

    private fun createSecretKey(): Key {
        val keyGeneratorParametersBuilder = KeyGenParameterSpec.Builder(
            NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )

        keyGeneratorParametersBuilder.setBlockModes(BLOCK_MODE)
        keyGeneratorParametersBuilder.setEncryptionPaddings(PADDING)
        keyGeneratorParametersBuilder.setKeySize(SIZE)
        keyGeneratorParametersBuilder.setUserAuthenticationRequired(true) // Require BIOMETRIC_STRONG auth before accessing

        val keyGeneratorParameters = keyGeneratorParametersBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, KEY_STORE_NAME)
        keyGenerator.init(keyGeneratorParameters)

        return keyGenerator.generateKey()
    }

    private fun getInitializedCipherForDecryption(initializationVector: ByteArray): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()

        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(initializationVector))
        return cipher
    }

    private fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, createIv(cipher.blockSize))
        return cipher
    }

    private fun getCipher(): Cipher {
        val transformation = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        return Cipher.getInstance(transformation)
    }

    private fun getOrCreateSecretKey(): Key {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME)
        keyStore.load(null) // Keystore must be loaded before it can be accessed

        return keyStore.getKey(NAME, null) ?: createSecretKey()
    }
}

data class EncryptedData(
    val cipherText: ByteArray,
    val initializationVector: ByteArray
) {
    companion object {
        fun fromString(encodedString: String): EncryptedData? {
            val parts = encodedString.split(DELIMITER);
            if (parts.size != 2) return null

            val cipherText = Base64.decode(parts[0], ENCODING)
            val iv = Base64.decode(parts[1], ENCODING)

            return EncryptedData(
                cipherText = cipherText,
                initializationVector = iv
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!cipherText.contentEquals(other.cipherText)) return false
        if (!initializationVector.contentEquals(other.initializationVector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cipherText.contentHashCode()
        result = 31 * result + initializationVector.contentHashCode()
        return result
    }

    override fun toString(): String {
        val base64CipherText = Base64.encodeToString(cipherText, ENCODING)
        val base64IV = Base64.encodeToString(initializationVector, ENCODING)

        return "$base64CipherText$DELIMITER$base64IV"
    }
}
