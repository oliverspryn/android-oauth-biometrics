package com.oliverspryn.android.oauthbiometrics.di.forwarders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import javax.inject.Inject

class PendingIntentForwarder @Inject constructor() {

    fun getActivity(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        flags
    )
}
