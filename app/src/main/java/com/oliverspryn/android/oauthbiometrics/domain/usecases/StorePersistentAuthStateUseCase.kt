package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import javax.inject.Inject

class StorePersistentAuthStateUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore,
    private val rxJavaFactory: RxJavaFactory
) {

    operator fun invoke(
        isEnabled: Boolean,
        serializedAuthState: String? = null,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)

        val operation = if (isEnabled && serializedAuthState != null) {
            persistentDataStore.writeAsync(key, serializedAuthState)
        } else {
            persistentDataStore.deleteAsync(key)
        }

        operation
            .subscribeOn(rxJavaFactory.io)
            .subscribe({ onComplete() }, { onError(it) })
    }
}
