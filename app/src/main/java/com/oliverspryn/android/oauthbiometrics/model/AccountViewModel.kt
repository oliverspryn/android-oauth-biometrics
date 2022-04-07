package com.oliverspryn.android.oauthbiometrics.model

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.MainActivity
import com.oliverspryn.android.oauthbiometrics.data.AuthZeroRepository
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography.UnableToEncryptData
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.cryptography.UnableToInitializeCipher
import com.oliverspryn.android.oauthbiometrics.domain.exceptions.storage.UnableToDecodeVolatileAuthState
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.BiometricResult
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.CreateBiometricPromptInfoForEnableBiometricLoginUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.biometrics.DidHandleEnrollmentInternally
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
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.crypto.Cipher
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

    fun dismissBiometricLockoutRationalePrompt() {
        viewModelState.update {
            it.copy(
                showBiometricLockoutRationalePrompt = false
            )
        }
    }

    fun dismissDeviceEnrollmentPrompt() {
        viewModelState.update {
            it.copy(
                showDeviceSecurityEnrollmentPrompt = false
            )
        }
    }

    fun enrollBiometrics() {
        val enrollment = enrollDeviceSecurityUseCase()

        if (enrollment is DidHandleEnrollmentInternally.No) {
            viewModelState.update {
                it.copy(
                    showDeviceSecurityEnrollmentPrompt = true
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

        Single
            .create<Cipher> { singleEmitter ->
                // Get the error handling for these functions in line with the observable stream
                // Allows for proper error handling down the stream's error handler
                singleEmitter.onSuccess(cryptographyManager.getInitializedCipherForEncryption())
            }
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .flatMapObservable { cipher ->
                val promptInfo = createBiometricPromptInfoForEnableBiometricLoginUseCase()

                presentBiometricPromptForCipherUseCase(
                    activity = activity,
                    promptInfo = promptInfo,
                    cipher = cipher
                )
            }
            .observeOn(rxJavaFactory.io)
            .flatMap { biometricOutcome ->
                if (biometricOutcome is BiometricResult.Success) {
                    val authState = authStateManager.serializedAuthState
                        ?: return@flatMap Observable.error(UnableToDecodeVolatileAuthState)

                    val cipher = biometricOutcome.result.cryptoObject?.cipher
                        ?: return@flatMap Observable.error(UnableToInitializeCipher())

                    val data = cryptographyManager.encryptData(authState, cipher)

                    storePersistentAuthStateUseCase(data.toString())
                        .andThen(Observable.just(biometricOutcome))
                } else {
                    Observable.just(biometricOutcome)
                }
            }
            .observeOn(rxJavaFactory.ui)
            .subscribe({ biometricOutcome ->
                if (biometricOutcome is BiometricResult.Success) {
                    val type = when (biometricOutcome.result.authenticationType) {
                        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "Biometric"
                        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "Device Credential"
                        else -> "Unknown"
                    }

                    Toast.makeText(
                        activity,
                        "Authenticated with: $type",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Can react here for something like a successful or bad fingerprint
                // Not yet a terminating condition
            }, { error ->
                val isBiometricError = error is BiometricResult.Error && error.isBiometricLockout
                val isCipherError =
                    error is UnableToEncryptData || error is UnableToInitializeCipher
                val isDecodeError = error is UnableToDecodeVolatileAuthState

                if (isBiometricError) {
                    showBiometricLockoutRationalePrompt()
                }

                if (isBiometricError || isCipherError || isDecodeError) {
                    disableBiometricLogin()
                }
            }, {
                viewModelState.update {
                    it.copy(isBiometricLoginOptionChecked = true)
                }
            })
    }

    fun updateBiometricsOptionAvailability() {
        val availableAuthenticators = strongestAvailableAuthenticationTypeUseCase()

        viewModelState.update {
            it.copy(
                isBiometricLoginFeatureAvailable = availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.Available,
                userNeedsToRegisterDeviceSecurity = availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
            )
        }
    }

    private fun checkBiometricLoginState() {
        hasBiometricLoginEnabledUseCase()
            .subscribeOn(rxJavaFactory.io)
            .observeOn(rxJavaFactory.ui)
            .flatMap { isEnabled ->
                val availableAuthenticators = strongestAvailableAuthenticationTypeUseCase()

                // Clears any cached state if the app remembers that the user enabled biometric login
                // but the user later cleared the biometrics/device credentials and decided to add
                // them back in at the behest of the app's prompt.
                // Otherwise, we would be trying to use data associated with an old key from the
                // previous credential set.
                // Only delete the old data set, but don't do a full logout, since that would delete
                // the volatile auth state.
                if (isEnabled && availableAuthenticators !is StrongestAvailableAuthenticationTypeForCryptography.Available) {
                    deletePersistentAuthStateUseCase()
                        .andThen(Single.just(false))
                } else {
                    Single.just(isEnabled)
                }
            }
            .subscribe({ isEnabled ->
                val availableAuthenticators = strongestAvailableAuthenticationTypeUseCase()
                val authenticationOptions =
                    if (availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.Available) {
                        val stringBuilder = StringBuilder()

                        if (availableAuthenticators.allowsBiometricStrong) stringBuilder.append("Biometric Strong, ")
                        if (availableAuthenticators.allowsBiometricWeak) stringBuilder.append("Biometric Weak, ")
                        if (availableAuthenticators.allowsDeviceCredentials) stringBuilder.append("Device Credentials")

                        stringBuilder.toString().trim().trimEnd(',')
                    } else {
                        ""
                    }

                viewModelState.update {
                    it.copy(
                        isBiometricLoginFeatureAvailable = availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.Available,
                        isBiometricLoginOptionChecked = isEnabled,
                        supportedBiometricClassifiers = authenticationOptions,
                        userNeedsToRegisterDeviceSecurity = availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
                    )
                }
            }, {
                val availableAuthenticators = strongestAvailableAuthenticationTypeUseCase()

                viewModelState.update {
                    it.copy(
                        isBiometricLoginFeatureAvailable = availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.Available,
                        isBiometricLoginOptionChecked = false,
                        supportedBiometricClassifiers = "",
                        userNeedsToRegisterDeviceSecurity = availableAuthenticators is StrongestAvailableAuthenticationTypeForCryptography.NothingEnrolled
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

    private fun showBiometricLockoutRationalePrompt() {
        viewModelState.update {
            it.copy(
                showBiometricLockoutRationalePrompt = true
            )
        }
    }
}

data class AccountUiState(
    val isBiometricLoginFeatureAvailable: Boolean = true,
    val isBiometricLoginOptionChecked: Boolean = false,
    val showBiometricLockoutRationalePrompt: Boolean = false,
    val showDeviceSecurityEnrollmentPrompt: Boolean = false,
    val supportedBiometricClassifiers: String = "",
    val userInfo: UserInfoResponse = UserInfoResponse.NoData,
    val userNeedsToRegisterDeviceSecurity: Boolean = false
) {

    sealed interface UserInfoResponse {
        object NoData : UserInfoResponse
        data class WithData(val payload: AuthZeroRepository.UserInfo) : UserInfoResponse
    }
}
