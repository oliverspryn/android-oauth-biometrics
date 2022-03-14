package com.oliverspryn.android.oauthbiometrics.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.di.factories.RxJavaFactory
import com.oliverspryn.android.oauthbiometrics.domain.usecases.InitializeOAuthLoginFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val initializeOAuthLoginFlowUseCase: InitializeOAuthLoginFlowUseCase,
    private val rxJavaFactory: RxJavaFactory
) : ViewModel() {
    private val viewModelState = MutableStateFlow(StartUiState())

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            StartUiState()
        )

    fun doLogin() {
        initializeOAuthLoginFlowUseCase()
            .subscribeOn(rxJavaFactory.io)
            .doOnSubscribe {
                rxJavaFactory.io.scheduleDirect {
                    viewModelState.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }
            }
            .doOnComplete {
                rxJavaFactory.io.scheduleDirect {
                    viewModelState.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
            }
            .doOnError {
                rxJavaFactory.io.scheduleDirect {
                    viewModelState.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
            }
            .subscribe({

            }, {

            })
    }

    fun setReauthEnabled() {
        viewModelState.update {
            it.copy(
                isReauthEnabled = true
            )
        }
    }

    fun setReauthDisabled() {
        viewModelState.update {
            it.copy(
                isReauthEnabled = false
            )
        }
    }
}

data class StartUiState(
    val isLoading: Boolean = false,
    val isReauthEnabled: Boolean = false
)