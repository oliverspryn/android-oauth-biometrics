package com.oliverspryn.android.oauthbiometrics.di.factories

import android.content.Context
import android.content.Intent
import javax.inject.Inject

class IntentFactory @Inject constructor() {

    fun newInstance(
        action: String
    ) = Intent(
        action
    )

    fun newInstance(
        packageContext: Context,
        cls: Class<*>
    ) = Intent(
        packageContext,
        cls
    )
}
