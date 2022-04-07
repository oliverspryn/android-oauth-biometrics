package com.oliverspryn.android.oauthbiometrics.utils.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.oliverspryn.android.oauthbiometrics.di.factories.IvParameterSpecFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.StringFactory
import com.oliverspryn.android.oauthbiometrics.di.forwarders.CipherForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.KeyGenParameterSpecBuilderForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.KeyGeneratorForwarder
import com.oliverspryn.android.oauthbiometrics.di.forwarders.KeyStoreForwarder
import com.oliverspryn.android.oauthbiometrics.di.modules.BuildModule
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography.UnableToDecryptData
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography.UnableToEncryptData
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography.UnableToInitializeCipher
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.BYTE_ARRAY_ENCODING
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Base64.DELIMITER
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Base64.ENCODING
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Encryption.ALGORITHM
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Encryption.BLOCK_MODE
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Encryption.PADDING
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.KEY_STORE_NAME
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Key.NAME
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyConfig.Key.SIZE
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.InvalidKeyException
import java.security.KeyStoreException
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Named

class CryptographyManager @Inject constructor(
    private val cipherForwarder: CipherForwarder,
    @ApplicationContext private val context: Context,
    private val ivParameterSpecFactory: IvParameterSpecFactory,
    private val keyGeneratorForwarder: KeyGeneratorForwarder,
    private val keyGenParameterSpecBuilderForwarder: KeyGenParameterSpecBuilderForwarder,
    private val keyStoreForwarder: KeyStoreForwarder,
    @Named(BuildModule.SDK_INT) private val sdkInt: Int,
    private val stringFactory: StringFactory
) {

    fun decryptData(cipherText: ByteArray, cipher: Cipher): String {
        try {
            val plaintext = cipher.doFinal(cipherText)
            return stringFactory.newInstance(plaintext, BYTE_ARRAY_ENCODING)
        } catch (e: IllegalBlockSizeException) {
            throw UnableToDecryptData(e)
        }
    }

    fun encryptData(plaintext: String, cipher: Cipher): EncryptedData {
        try {
            val cipherText = cipher.doFinal(plaintext.toByteArray(BYTE_ARRAY_ENCODING))

            return EncryptedData(
                cipherText = cipherText,
                iv = cipher.iv
            )
        } catch (e: IllegalBlockSizeException) {
            throw UnableToEncryptData(e)
        }
    }

    fun getInitializedCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = getCipher()

        try {
            val secretKey = getOrCreateSecretKey()

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                ivParameterSpecFactory.newInstance(iv)
            )
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw UnableToInitializeCipher(e)
        } catch (e: InvalidKeyException) {
            throw UnableToInitializeCipher(e)
        }

        return cipher
    }

    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipher()

        val secretKey = try {
            getOrCreateSecretKey()
        } catch (e: KeyPermanentlyInvalidatedException) {
            tryDeleteAndRecreateKey()
        } catch (e: InvalidKeyException) {
            tryDeleteAndRecreateKey()
        }

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    @SuppressLint("NewApi") // Lint can't tell I've accounted for this via DI
    @Suppress("DEPRECATION") // setUserAuthenticationValidityDurationSeconds needed, but not available for < API 30
    private fun createSecretKey(): SecretKey {
        val keyGeneratorParameters = keyGenParameterSpecBuilderForwarder
            .builder(
                keystoreAlias = NAME,
                purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(BLOCK_MODE)
                setEncryptionPaddings(PADDING)
                setKeySize(SIZE)
                setRandomizedEncryptionRequired(true)
                setUserAuthenticationRequired(true)

                if (sdkInt >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                    setUserAuthenticationValidWhileOnBody(false)
                }

                if (sdkInt >= Build.VERSION_CODES.P) {
                    val hasStrongBox = context
                        .packageManager
                        .hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

                    if (hasStrongBox) setIsStrongBoxBacked(true)
                }
            }
            .build()

        val keyGenerator = keyGeneratorForwarder.getInstance(ALGORITHM, KEY_STORE_NAME)
        keyGenerator.init(keyGeneratorParameters)

        return keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        val transformation = "$ALGORITHM/$BLOCK_MODE/$PADDING"
        return cipherForwarder.getInstance(transformation)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = keyStoreForwarder.getInstance(KEY_STORE_NAME)
        keyStore.load(null) // Keystore must be loaded before it can be accessed

        return if (keyStore.containsAlias(NAME)) {
            try {
                val key = keyStore.getKey(NAME, null)
                key as SecretKey
            } catch (e: UnrecoverableKeyException) {
                tryDeleteAndRecreateKey()
            } catch (e: KeyStoreException) {
                tryDeleteAndRecreateKey()
            }
        } else {
            createSecretKey()
        }
    }

    private fun tryDeleteAndRecreateKey(): SecretKey {
        val keyStore = keyStoreForwarder.getInstance(KEY_STORE_NAME)
        keyStore.load(null) // Keystore must be loaded before it can be accessed

        try {
            keyStore.deleteEntry(NAME)
        } catch (e: KeyStoreException) {
            // If the key cannot be deleted, then create a new one in its place
        }

        return createSecretKey()
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
