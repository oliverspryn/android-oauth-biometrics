package com.oliverspryn.android.oauthbiometrics.domain.usecases.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class HasBiometricLoginEnabledUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore
) {

    operator fun invoke(): Single<Boolean> {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)

        return persistentDataStore
            .readAsync(key)
            .toSingle()
            .onErrorResumeNext {
                Single.just("")
            }
            .flatMap { authState ->
                val hasData = !authState.isNullOrBlank()
                Single.just(hasData)
            }
    }
}
