package com.oliverspryn.android.oauthbiometrics.utils

import androidx.compose.runtime.*
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