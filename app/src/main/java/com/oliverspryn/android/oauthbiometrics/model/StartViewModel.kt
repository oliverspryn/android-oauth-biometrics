package com.oliverspryn.android.oauthbiometrics.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.InitializeOAuthLoginFlowUseCase
import com.oliverspryn.android.oauthbiometrics.domain.usecases.LaunchOAuthLoginFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.openid.appauth.AuthorizationRequest
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    initializeOAuthLoginFlowUseCase: InitializeOAuthLoginFlowUseCase,
    private val launchOAuthLoginFlowUseCase: LaunchOAuthLoginFlowUseCase,
    rxJavaFactory: RxJavaFactory
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
        initializeOAuthLoginFlowUseCase()
            .subscribeOn(rxJavaFactory.io)
            .doOnSubscribe { setIsLoading() }
            .doOnSuccess { setIsLoadingComplete() }
            .doOnError { setIsLoadingComplete() }
            .subscribe({
                authRequest = it
            }, {

            })
    }

    fun doLogin() {
        authRequest?.let { request ->
            launchOAuthLoginFlowUseCase(request)
        }
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
}

data class StartUiState(
    val isLoading: Boolean = true,
    val isReauthEnabled: Boolean = false
)