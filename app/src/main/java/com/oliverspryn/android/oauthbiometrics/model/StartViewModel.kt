package com.oliverspryn.android.oauthbiometrics.model

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography.UnableToInitializeCipher
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.storage.UnableToDecodePersistentAuthState
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.BiometricResult
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.CreateBiometricPromptInfoToPerformBiometricLoginUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.PresentBiometricPromptForCipherUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.StrongestAvailableAuthenticationTypeForCryptography
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.InitializeOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.LaunchOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.GetPersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.HasBiometricLoginEnabledUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.LogoutUseCase
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
    private val getPersistentAuthStateUseCase: GetPersistentAuthStateUseCase,
    private val hasBiometricLoginEnabledUseCase: HasBiometricLoginEnabledUseCase,
    private val initializeOAuthLoginFlowUseCase: InitializeOAuthLoginFlowUseCase,
    private val launchOAuthLoginFlowUseCase: LaunchOAuthLoginFlowUseCase,
    private val logoutUseCase: LogoutUseCase,
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

    fun dismissBiometricLockoutRationalePrompt() {
        viewModelState.update {
            it.copy(
                showBiometricLockoutRationalePrompt = false
            )
        }
    }

    fun dismissWebLoginRationalePrompt() {
        viewModelState.update {
            it.copy(
                showWebLoginRationalePrompt = false
            )
        }
    }

    fun doBiometricLogin(activity: FragmentActivity, onLoginSuccess: () -> Unit) {
        getPersistentAuthStateUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .flatMapObservable { encryptedData ->
                val cipher = cryptographyManager.getInitializedCipherForDecryption(encryptedData.iv)
                val promptInfo = createBiometricPromptInfoToPerformBiometricLoginUseCase()

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

                // Alternative condition is something like a bad fingerprint
                // Not an error yet
            }, { error ->
                val isBiometricError = error is BiometricResult.Error && error.isBiometricLockout
                val isCipherError = error is UnableToInitializeCipher
                val isDecodeError = error is UnableToDecodePersistentAuthState

                if (isBiometricError) {
                    disableBiometricLoginButton()
                    showBiometricLockoutRationalePrompt()
                } else if (isCipherError || isDecodeError) {
                    disableBiometricLoginButton()
                    doLogout()
                    showWebLoginRationalePrompt()
                }
            }, {
                // Login success, handled with the data provided in onNext()
            })
    }

    fun doWebLogin() {
        authRequest?.let { request ->
            launchOAuthLoginFlowUseCase(request)
        }
    }

    private fun disableBiometricLoginButton() {
        viewModelState.update {
            it.copy(isBiometricLoginEnabled = false)
        }
    }

    private fun disableWebLoginButton() {
        viewModelState.update {
            it.copy(
                isWebLoginEnabled = false
            )
        }
    }

    private fun doLogout() {
        logoutUseCase(false)
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({
                // Best effort
            }, {
                // Ignore the outcomes :)
            })
    }

    private fun enableWebLoginButton() {
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
                disableWebLoginButton()
                setIsLoading()
            }
            .doOnSuccess {
                enableWebLoginButton()
                setIsLoadingComplete()
            }
            .doOnError {
                enableWebLoginButton()
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

    private fun showBiometricLockoutRationalePrompt() {
        viewModelState.update {
            it.copy(
                showBiometricLockoutRationalePrompt = true
            )
        }
    }

    private fun showWebLoginRationalePrompt() {
        viewModelState.update {
            it.copy(
                showWebLoginRationalePrompt = true
            )
        }
    }

    private fun storeAuthStateInStaticMemory(
        cipherText: ByteArray,
        result: BiometricPrompt.AuthenticationResult
    ) {
        val cipher = result.cryptoObject?.cipher ?: return
        authStateManager.serializedAuthState = cryptographyManager.decryptData(cipherText, cipher)
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
                doLogout() // In case biometrics were previously available, user logged in, then removed biometrics

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
    val isWebLoginEnabled: Boolean = false,
    val showBiometricLockoutRationalePrompt: Boolean = false,
    val showWebLoginRationalePrompt: Boolean = false
)

private data class BiometricLoginPayload(
    val biometricResult: BiometricResult,
    val encryptedData: EncryptedData
)
