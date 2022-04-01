package com.oliverspryn.android.oauthbiometrics.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.coroutines.CoroutineScope

@Composable
@NonRestartableComposable
fun RunOnceEffect(
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(
        key1 = Unit,
        block = block
    )
}
