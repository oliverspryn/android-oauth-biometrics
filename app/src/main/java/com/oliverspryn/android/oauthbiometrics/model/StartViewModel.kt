package com.oliverspryn.android.oauthbiometrics.model

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.CreateBiometricPromptInfoToPerformBiometricLoginUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.DeletePersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.GetPersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.HasBiometricLoginEnabledUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.InitializeOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.LaunchOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.PresentBiometricPromptForCipherUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.StrongestAvailableAuthenticationTypeForCryptography
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
        getPersistentAuthStateUseCase(
            onComplete = { encryptedData ->
                val cipher = cryptographyManager.getInitializedCipherForDecryption(encryptedData.iv)
                val promptInfo = createBiometricPromptInfoToPerformBiometricLoginUseCase()
                setIsLoading()

                presentBiometricPromptForCipherUseCase(
                    activity = activity,
                    promptInfo = promptInfo,
                    cipher = cipher,
                    onSuccess = { result ->
                        storeAuthStateInStaticMemory(encryptedData.cipherText, result)
                        setIsLoadingComplete()
                        onLoginSuccess()
                    },
                    onFailed = {
                        setIsLoadingComplete()
                    },
                    onError = { errorCode, _ ->
                        if (errorCode.isBiometricLockout()) {
                            disableBiometricLogin()
                        }

                        setIsLoadingComplete()
                    }
                )
            },
            onError = {

            }
        )
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
        deletePersistentAuthStateUseCase(
            onComplete = {
                viewModelState.update {
                    it.copy(isBiometricLoginEnabled = false)
                }
            },
            onError = { }
        )
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

        hasBiometricLoginEnabledUseCase { appBiometricLoginEnabled ->
            viewModelState.update {
                it.copy(
                    isBiometricLoginEnabled = biometricsAvailableFromSystem && appBiometricLoginEnabled
                )
            }
        }
    }
}

private fun Int.isBiometricLockout(): Boolean =
    this == BiometricPrompt.ERROR_LOCKOUT || this == BiometricPrompt.ERROR_LOCKOUT_PERMANENT

data class StartUiState(
    val isBiometricLoginEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val isWebLoginEnabled: Boolean = false
)
