package com.oliverspryn.android.oauthbiometrics.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class StartViewModel: ViewModel() {
    private val viewModelState = MutableStateFlow(StartUiState())

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            StartUiState()
        )

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
    val isReauthEnabled: Boolean = false
)