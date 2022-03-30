package com.oliverspryn.android.oauthbiometrics.model

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.domain.usecases.CreateBiometricPromptInfoForEnableBiometricLoginUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.EnrollDeviceSecurityUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.HasBiometricLoginEnabledUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.OpenAndroidSecuritySettingsUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.PresentBiometricPromptForCipherUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.StorePersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.StrongestAvailableAuthenticationTypeForCryptography
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authStateManager: AuthStateManager,
    private val createBiometricPromptInfoForEnableBiometricLoginUseCase: CreateBiometricPromptInfoForEnableBiometricLoginUseCase,
    private val cryptographyManager: CryptographyManager,
    private val enrollDeviceSecurityUseCase: EnrollDeviceSecurityUseCase,
    private val hasBiometricLoginEnabledUseCase: HasBiometricLoginEnabledUseCase,
    private val openAndroidSecuritySettingsUseCase: OpenAndroidSecuritySettingsUseCase,
    private val presentBiometricPromptForCipherUseCase: PresentBiometricPromptForCipherUseCase,
    private val storePersistentAuthStateUseCase: StorePersistentAuthStateUseCase,
    private val strongestAvailableAuthenticationTypeUseCase: ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
) : ViewModel() {

    private val viewModelState = MutableStateFlow(AccountUiState())

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AccountUiState()
        )

    init {
        checkBiometricLoginState()
    }

    fun dismissDeviceEnrollmentDialog() {
        viewModelState.update {
            it.copy(
                showDeviceSecurityEnrollmentDialog = false
            )
        }
    }

    fun enrollBiometrics() {
        val canEnrollDirectly = enrollDeviceSecurityUseCase()

        if (!canEnrollDirectly) {
            viewModelState.update {
                it.copy(
                    showDeviceSecurityEnrollmentDialog = true
                )
            }
        }
    }

    fun evaluateBiometricsState() {
        viewModelState.update {
            it.copy(
                isBiometricLoginFeatureAvailable = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
            )
        }
    }

    fun goToAndroidSecuritySettings() {
        openAndroidSecuritySettingsUseCase()
    }

    fun setBiometricLoginFeatureEnabled(isEnabled: Boolean, activity: FragmentActivity) {
        if (!isEnabled) {
            disableBiometricLogin()
            return
        }

        val cipher = cryptographyManager.getInitializedCipherForEncryption()
        val promptInfo = createBiometricPromptInfoForEnableBiometricLoginUseCase()

        presentBiometricPromptForCipherUseCase(
            activity = activity,
            promptInfo = promptInfo,
            cipher = cipher,
            onSuccess = { result -> enableBiometricLogin(result) },
            onFailed = { disableBiometricLogin() },
            onError = { _, _ -> disableBiometricLogin() }
        )
    }

    private fun checkBiometricLoginState() {
        hasBiometricLoginEnabledUseCase(
            isEnabled = { enabled ->
                viewModelState.update {
                    it.copy(
                        isBiometricLoginFeatureAvailable = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                        isBiometricLoginOptionChecked = enabled,
                        userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
                    )
                }
            }
        )
    }

    private fun disableBiometricLogin() {
        storePersistentAuthStateUseCase(
            isEnabled = false,
            serializedAuthState = null,
            onComplete = {
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = false)
                }
            },
            onError = { }
        )
    }

    private fun enableBiometricLogin(result: BiometricPrompt.AuthenticationResult) {
        val authState = authStateManager.serializedAuthState ?: return
        val cipher = result.cryptoObject?.cipher ?: return
        val data = cryptographyManager.encryptData(authState, cipher)

        storePersistentAuthStateUseCase(
            isEnabled = true,
            serializedAuthState = data.toString(),
            onComplete = {
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = true)
                }
            },
            onError = { }
        )
    }
}

data class AccountUiState(
    val isBiometricLoginFeatureAvailable: Boolean = true,
    val isBiometricLoginOptionChecked: Boolean = false,
    val showDeviceSecurityEnrollmentDialog: Boolean = false,
    val userNeedsToRegisterDeviceSecurity: Boolean = false
)
