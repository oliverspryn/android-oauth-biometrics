package com.oliverspryn.android.oauthbiometrics.model

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.domain.usecases.CreateBiometricPromptInfoForEnableReauthenticationUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.EnrollDeviceSecurityUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.OpenAndroidSecuritySettingsUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.PresentBiometricPromptForCipherUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.StrongestAvailableAuthenticationTypeForCryptography
import com.oliverspryn.android.oauthbiometrics.utils.AuthStateManager
import com.oliverspryn.android.oauthbiometrics.utils.security.CryptographyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authStateManager: AuthStateManager,
    private val createBiometricPromptInfoForEnableReauthenticationUseCase: CreateBiometricPromptInfoForEnableReauthenticationUseCase,
    private val cryptographyManager: CryptographyManager,
    private val enrollDeviceSecurityUseCase: EnrollDeviceSecurityUseCase,
    private val openAndroidSecuritySettingsUseCase: OpenAndroidSecuritySettingsUseCase,
    private val presentBiometricPromptForCipherUseCase: PresentBiometricPromptForCipherUseCase,
    private val strongestAvailableAuthenticationTypeUseCase: ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
) : ViewModel() {

    private val viewModelState = MutableStateFlow(
        AccountUiState(
            isReauthenticationFeatureEnabled = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
            userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
        )
    )

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AccountUiState(
                isReauthenticationFeatureEnabled = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
            )
        )

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
                isReauthenticationFeatureEnabled = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
            )
        }
    }

    fun goToAndroidSecuritySettings() {
        openAndroidSecuritySettingsUseCase()
    }

    fun setReauthenticationFeatureEnabled(isEnabled: Boolean, activity: FragmentActivity) {
        if (!isEnabled) {
            disableReauthentication()
            return
        }

        val cipher = cryptographyManager.getInitializedCipherForEncryption()
        val promptInfo = createBiometricPromptInfoForEnableReauthenticationUseCase()

        presentBiometricPromptForCipherUseCase(
            activity = activity,
            promptInfo = promptInfo,
            cipher = cipher,
            onSuccess = { result -> enableReauthentication(result) },
            onFailed = { disableReauthentication() },
            onError = { errorCode, description -> disableReauthentication() }
        )
    }

    private fun disableReauthentication() {
        viewModelState.update {
            it.copy(isReauthenticationOptionChecked = false)
        }
    }

    private fun enableReauthentication(result: BiometricPrompt.AuthenticationResult) {
        val authState = authStateManager.getSerializedAuthState() ?: return
        val cipher = result.cryptoObject?.cipher ?: return
        val data = cryptographyManager.encryptData(authState, cipher)
        Logger.getGlobal().log(Level.INFO, data.toString())

        viewModelState.update {
            it.copy(isReauthenticationOptionChecked = true)
        }
    }
}

data class AccountUiState(
    val isReauthenticationFeatureEnabled: Boolean = true,
    val isReauthenticationOptionChecked: Boolean = false,
    val showDeviceSecurityEnrollmentDialog: Boolean = false,
    val userNeedsToRegisterDeviceSecurity: Boolean = false
)
