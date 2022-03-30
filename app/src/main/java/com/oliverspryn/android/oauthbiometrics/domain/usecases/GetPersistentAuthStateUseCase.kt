package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.UnableToDecodePersistentAuthState
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import com.oliverspryn.android.oauthbiometrics.utils.security.EncryptedData
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class GetPersistentAuthStateUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore,
    private val rxJavaFactory: RxJavaFactory
) {

    operator fun invoke(
        onComplete: (EncryptedData) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)

        persistentDataStore
            .readAsync(key)
            .toSingle()
            .onErrorResumeNext {
                Single.just("")
            }
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({ authState ->
                val data = EncryptedData.fromString(authState)

                if (data != null) {
                    onComplete(data)
                } else {
                    onError(UnableToDecodePersistentAuthState)
                }
            }, {
                onError(it)
            })
    }
}
