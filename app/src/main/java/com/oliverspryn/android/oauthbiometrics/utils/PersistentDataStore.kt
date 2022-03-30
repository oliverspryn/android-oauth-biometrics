package com.oliverspryn.android.oauthbiometrics.utils

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class PersistentDataStore @Inject constructor(
    private val dataStore: RxDataStore<Preferences>
) {

    fun <ValueType : Any> deleteAsync(
        key: Preferences.Key<ValueType>
    ): Completable = dataStore
        .updateDataAsync { preferencesObject ->
            val mutablePreferences = preferencesObject.toMutablePreferences()
            mutablePreferences.remove(key)
            Single.just(mutablePreferences)
        }
        .ignoreElement()

    fun <ValueType : Any> readAsync(
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
