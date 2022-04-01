package com.oliverspryn.android.oauthbiometrics.domain.usecases.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import io.reactivex.rxjava3.core.Completable
import javax.inject.Inject

class DeletePersistentAuthStateUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore
) {

    operator fun invoke(): Completable {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)
        return persistentDataStore.deleteAsync(key)
    }
}
