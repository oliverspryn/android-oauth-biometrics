package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.content.Context
import androidx.core.content.ContextCompat
import javax.inject.Inject

class ContextCompatForwarder @Inject constructor() {
    fun getMainExecutor(context: Context) = ContextCompat.getMainExecutor(context)
}
