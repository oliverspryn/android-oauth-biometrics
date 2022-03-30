package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import javax.inject.Inject

class DeletePersistentAuthStateUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore,
    private val rxJavaFactory: RxJavaFactory
) {

    operator fun invoke(
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)

        persistentDataStore
            .deleteAsync(key)
            .subscribeOn(rxJavaFactory.io)
            .subscribe({ onComplete() }, { onError(it) })
    }
}
