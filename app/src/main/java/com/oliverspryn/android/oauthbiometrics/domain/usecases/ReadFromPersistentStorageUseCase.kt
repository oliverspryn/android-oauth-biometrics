package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Maybe
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class ReadFromPersistentStorageUseCase @Inject constructor(
    private val dataStore: RxDataStore<Preferences>
) {

    operator fun <ValueType : Any> invoke(
        key: Preferences.Key<ValueType>
    ): Maybe<ValueType> = dataStore
        .data()
        .firstOrError()
        .flatMapMaybe { preferences ->
            preferences[key]?.let { value ->
                return@flatMapMaybe Maybe.just(value)
            }

            Maybe.empty()
        }
        .onErrorResumeNext {
            Maybe.empty()
        }
}
