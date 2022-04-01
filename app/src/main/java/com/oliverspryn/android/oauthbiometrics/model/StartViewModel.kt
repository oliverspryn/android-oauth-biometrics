package com.oliverspryn.android.oauthbiometrics.model

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.BiometricResult
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.CreateBiometricPromptInfoToPerformBiometricLoginUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.PresentBiometricPromptForCipherUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.StrongestAvailableAuthenticationTypeForCryptography
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.InitializeOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.LaunchOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.DeletePersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.GetPersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.HasBiometricLoginEnabledUseCase
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyManager
import com.oliverspryn.android.oauthbiometrics.utils.security.EncryptedData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.openid.appauth.AuthorizationRequest
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val authStateManager: AuthStateManager,
    private val createBiometricPromptInfoToPerformBiometricLoginUseCase: CreateBiometricPromptInfoToPerformBiometricLoginUseCase,
    private val cryptographyManager: CryptographyManager,
    private val deletePersistentAuthStateUseCase: DeletePersistentAuthStateUseCase,
    private val getPersistentAuthStateUseCase: GetPersistentAuthStateUseCase,
    private val hasBiometricLoginEnabledUseCase: HasBiometricLoginEnabledUseCase,
    private val initializeOAuthLoginFlowUseCase: InitializeOAuthLoginFlowUseCase,
    private val launchOAuthLoginFlowUseCase: LaunchOAuthLoginFlowUseCase,
    private val presentBiometricPromptForCipherUseCase: PresentBiometricPromptForCipherUseCase,
    private val rxJavaFactory: RxJavaFactory,
    private val strongestAvailableAuthenticationTypeUseCase: ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
) : ViewModel() {

    private var authRequest: AuthorizationRequest? = null
    private val viewModelState = MutableStateFlow(StartUiState())

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            StartUiState()
        )

    init {
        initializeOAuthLoginFlow()
        tryEnableBiometricLoginButton()
    }

    fun doBiometricLogin(activity: FragmentActivity, onLoginSuccess: () -> Unit) {
        getPersistentAuthStateUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .flatMapObservable { encryptedData ->
                val cipher = cryptographyManager.getInitializedCipherForDecryption(encryptedData.iv)
                val promptInfo = createBiometricPromptInfoToPerformBiometricLoginUseCase()
                setIsLoading()

                presentBiometricPromptForCipherUseCase(
                    activity = activity,
                    promptInfo = promptInfo,
                    cipher = cipher
                ).flatMap { biometricResult ->
                    Observable.just(
                        BiometricLoginPayload(
                            biometricResult = biometricResult,
                            encryptedData = encryptedData
                        )
                    )
                }
            }
            .subscribe({ biometricLoginPayload ->
                if (biometricLoginPayload.biometricResult is BiometricResult.Success) {
                    storeAuthStateInStaticMemory(
                        cipherText = biometricLoginPayload.encryptedData.cipherText,
                        result = biometricLoginPayload.biometricResult.result
                    )

                    onLoginSuccess()
                }

                // Alternative is something like a bad fingerprint
                // Not an error yet
                setIsLoadingComplete()
            }, { error ->
                if (error is BiometricResult.Error && error.isBiometricLockout) {
                    disableBiometricLogin()
                }

                setIsLoadingComplete()
            }, {
                // Login success, handled with the data provided in onNext()
            })
    }

    private fun storeAuthStateInStaticMemory(
        cipherText: ByteArray,
        result: BiometricPrompt.AuthenticationResult
    ) {
        val cipher = result.cryptoObject?.cipher ?: return
        authStateManager.serializedAuthState = cryptographyManager.decryptData(cipherText, cipher)
    }

    fun doWebLogin() {
        authRequest?.let { request ->
            launchOAuthLoginFlowUseCase(request)
        }
    }

    private fun disableBiometricLogin() {
        deletePersistentAuthStateUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({
                viewModelState.update {
                    it.copy(isBiometricLoginEnabled = false)
                }
            }, {
                viewModelState.update {
                    it.copy(isBiometricLoginEnabled = false)
                }
            })
    }

    private fun disableWebLogin() {
        viewModelState.update {
            it.copy(
                isWebLoginEnabled = true
            )
        }
    }

    private fun enableWebLogin() {
        viewModelState.update {
            it.copy(
                isWebLoginEnabled = true
            )
        }
    }

    private fun initializeOAuthLoginFlow() {
        initializeOAuthLoginFlowUseCase()
            .subscribeOn(rxJavaFactory.io)
            .doOnSubscribe {
                disableWebLogin()
                setIsLoading()
            }
            .doOnSuccess {
                enableWebLogin()
                setIsLoadingComplete()
            }
            .doOnError {
                enableWebLogin()
                setIsLoadingComplete()
            }
            .subscribe({
                authRequest = it
            }, {

            })
    }

    private fun setIsLoading() {
        viewModelState.update {
            it.copy(
                isLoading = true
            )
        }
    }

    private fun setIsLoadingComplete() {
        viewModelState.update {
            it.copy(
                isLoading = false
            )
        }
    }

    private fun tryEnableBiometricLoginButton() {
        val biometricsAvailableFromSystem =
            strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available

        hasBiometricLoginEnabledUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({ appBiometricLoginEnabled ->
                viewModelState.update {
                    it.copy(
                        isBiometricLoginEnabled = biometricsAvailableFromSystem && appBiometricLoginEnabled
                    )
                }
            }, {
                viewModelState.update {
                    it.copy(
                        isBiometricLoginEnabled = false
                    )
                }
            })
    }
}

data class StartUiState(
    val isBiometricLoginEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val isWebLoginEnabled: Boolean = false
)

private data class BiometricLoginPayload(
    val biometricResult: BiometricResult,
    val encryptedData: EncryptedData
)
