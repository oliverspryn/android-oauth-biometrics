package com.oliverspryn.android.oauthbiometrics.domain.usecases

import androidx.constraintlayout.solver.widgets.Optimizer.enabled
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.utils.PersistentDataStore
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class HasBiometricLoginEnabledUseCase @Inject constructor(
    private val persistentDataStore: PersistentDataStore,
    private val rxJavaFactory: RxJavaFactory
) {

    operator fun invoke(isEnabled: (Boolean) -> Unit) {
        val key = stringPreferencesKey(AuthConstants.KEY_NAME)

        persistentDataStore
            .readAsync(key)
            .toSingle()
            .onErrorResumeNext {
                Single.just("")
            }
            .flatMap { authState ->
                val hasData = !authState.isNullOrBlank()
                Single.just(hasData)
            }
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({ biometricLoginEnabled ->
                isEnabled(biometricLoginEnabled)
            }, {
                isEnabled(false)
            })
    }
}
