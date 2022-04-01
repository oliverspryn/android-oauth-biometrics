package com.oliverspryn.android.oauthbiometrics.model

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.TokenExchangeOutcome
import com.oliverspryn.android.oauthbiometrics.domain.usecases.oauth.TokenExchangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val tokenExchangeUseCase: TokenExchangeUseCase
) : ViewModel() {
    private val viewModelState = MutableStateFlow(LoadingUiState())

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            LoadingUiState(
                message = "Loading..."
            )
        )

    fun performTokenExchange(
        activity: FragmentActivity,
        onNotLoggingIn: () -> Unit,
        onLoginSuccess: () -> Unit
    ) {
        tokenExchangeUseCase(activity)
            .subscribe({ outcome ->
                if (outcome is TokenExchangeOutcome.LoginSuccess) {
                    onLoginSuccess()
                } else if (outcome is TokenExchangeOutcome.NotLoggingIn) {
                    onNotLoggingIn()
                }

            }, { error ->
                updateMessageWithError(error.message)
            })
    }

    private fun updateMessageWithError(message: String?) {
        viewModelState.update {
            it.copy(
                message = message ?: "Error"
            )
        }
    }
}

data class LoadingUiState(
    val message: String = ""
)
