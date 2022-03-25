package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class WriteToPersistentStorageUseCase @Inject constructor(
    private val dataStore: RxDataStore<Preferences>
) {

    fun <ValueType : Any> writeAsync(
        key: Preferences.Key<ValueType>,
        value: ValueType
    ): Completable = dataStore
        .updateDataAsync { preferencesObject ->
            val mutablePreferences = preferencesObject.toMutablePreferences()
            mutablePreferences[key] = value
            Single.just(mutablePreferences)
        }
        .ignoreElement()
}
