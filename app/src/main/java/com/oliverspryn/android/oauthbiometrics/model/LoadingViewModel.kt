package com.oliverspryn.android.oauthbiometrics.model

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oliverspryn.android.oauthbiometrics.domain.usecases.TokenExchangeUseCase
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
        activity: ComponentActivity,
        notLoggingIn: () -> Unit,
        loginSuccess: () -> Unit
    ) {
        tokenExchangeUseCase(
            activity = activity,
            notLoggingIn = notLoggingIn,
            loginSuccess = loginSuccess,
            loginError = { updateMessageWithError(it.message) }
        )
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
