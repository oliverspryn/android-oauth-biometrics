package com.oliverspryn.android.oauthbiometrics.model

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.data.AuthZeroRepository
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.BiometricResult
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.CreateBiometricPromptInfoForEnableBiometricLoginUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.EnrollDeviceSecurityUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.ObtainStrongestAvailableAuthenticationTypeForCryptographyUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.OpenAndroidSecuritySettingsUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.PresentBiometricPromptForCipherUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.StrongestAvailableAuthenticationTypeForCryptography
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.DidHandleLogoutWithOAuthRedirect
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.DeletePersistentAuthStateUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.HasBiometricLoginEnabledUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.LogoutUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.storage.StorePersistentAuthStateUseCase
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
    private val authZeroRepository: AuthZeroRepository,
    private val createBiometricPromptInfoForEnableBiometricLoginUseCase: CreateBiometricPromptInfoForEnableBiometricLoginUseCase,
    private val cryptographyManager: CryptographyManager,
    private val deletePersistentAuthStateUseCase: DeletePersistentAuthStateUseCase,
    private val enrollDeviceSecurityUseCase: EnrollDeviceSecurityUseCase,
    private val hasBiometricLoginEnabledUseCase: HasBiometricLoginEnabledUseCase,
    private val intentFactory: IntentFactory,
    private val logoutUseCase: LogoutUseCase,
    private val openAndroidSecuritySettingsUseCase: OpenAndroidSecuritySettingsUseCase,
    private val presentBiometricPromptForCipherUseCase: PresentBiometricPromptForCipherUseCase,
    private val rxJavaFactory: RxJavaFactory,
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
        getUserInfo()
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

    fun goToAndroidSecuritySettings() {
        openAndroidSecuritySettingsUseCase()
    }

    fun logout(context: Context) {
        val restartActivity = intentFactory
            .newInstance(context, MainActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }

        logoutUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({ outcome ->
                if (outcome is DidHandleLogoutWithOAuthRedirect.No) {
                    context.startActivity(restartActivity)
                }
            }, {
                context.startActivity(restartActivity)
            })
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
            cipher = cipher
        ).subscribe({ biometricOutcome ->
            if (biometricOutcome is BiometricResult.Success) {
                enableBiometricLogin(biometricOutcome.result)
            }

            // Alternative is something like a bad fingerprint
            // Do nothing, in that case
            // Not an error yet
        }, { error ->
            if (error is BiometricResult.Error && error.isBiometricLockout) {
                disableBiometricLogin()
            }
        }, {
            // Biometric success, handled with the data provided in onNext()
        })
    }

    fun updateBiometricsOption() {
        viewModelState.update {
            it.copy(
                isBiometricLoginFeatureAvailable = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
            )
        }
    }

    private fun checkBiometricLoginState() {
        hasBiometricLoginEnabledUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({ isEnabled ->
                viewModelState.update {
                    it.copy(
                        isBiometricLoginFeatureAvailable = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                        isBiometricLoginOptionChecked = isEnabled,
                        userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
                    )
                }
            }, {
                viewModelState.update {
                    it.copy(
                        isBiometricLoginFeatureAvailable = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.Available,
                        isBiometricLoginOptionChecked = false,
                        userNeedsToRegisterDeviceSecurity = strongestAvailableAuthenticationTypeUseCase() is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
                    )
                }
            })
    }

    private fun disableBiometricLogin() {
        deletePersistentAuthStateUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = false)
                }
            }, {
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = false)
                }
            })
    }

    private fun enableBiometricLogin(result: BiometricPrompt.AuthenticationResult) {
        val authState = authStateManager.serializedAuthState ?: return
        val cipher = result.cryptoObject?.cipher ?: return
        val data = cryptographyManager.encryptData(authState, cipher)

        storePersistentAuthStateUseCase(data.toString())
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = true)
                }
            }, {
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = false)
                }
            })
    }

    private fun getUserInfo() {
        authZeroRepository
            .getUserInfo()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .subscribe({ userInfo ->
                viewModelState.update {
                    it.copy(
                        userInfo = AccountUiState.UserInfoResponse.WithData(userInfo)
                    )
                }
            }, {
                // Won't show profile info
            })
    }
}

data class AccountUiState(
    val isBiometricLoginFeatureAvailable: Boolean = true,
    val isBiometricLoginOptionChecked: Boolean = false,
    val showDeviceSecurityEnrollmentDialog: Boolean = false,
    val userInfo: UserInfoResponse = UserInfoResponse.NoData,
    val userNeedsToRegisterDeviceSecurity: Boolean = false
) {

    sealed interface UserInfoResponse {
        object NoData : UserInfoResponse
        data class WithData(val payload: AuthZeroRepository.UserInfo) : UserInfoResponse
    }
}
