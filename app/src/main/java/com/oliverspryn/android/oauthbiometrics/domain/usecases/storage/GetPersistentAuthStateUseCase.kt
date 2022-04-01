package com.oliverspryn.android.oauthbiometrics.domain.usecases.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.storage.UnableToDecodePersistentAuthState
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import com.oliverspryn.android.oauthbiometrics.utils.security.EncryptedData
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class GetPersistentAuthStateUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore
) {

    operator fun invoke(): Single<EncryptedData> {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)

        return persistentDataStore
            .readAsync(key)
            .toSingle()
            .onErrorResumeNext {
                Single.just("")
            }
            .flatMap { authState ->
                val data = EncryptedData.fromString(authState)

                if (data != null) {
                    Single.just(data)
                } else {
                    Single.error(UnableToDecodePersistentAuthState)
                }
            }
    }
}
