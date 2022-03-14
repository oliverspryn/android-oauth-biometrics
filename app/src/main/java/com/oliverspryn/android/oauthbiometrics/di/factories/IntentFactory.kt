package com.oliverspryn.android.oauthbiometrics.di.factories

import android.content.Context
import android.content.Intent
import javax.inject.Inject

class IntentFactory @Inject constructor() {

    fun newInstance(
        packageContext: Context,
        cls: Class<*>
    ) = Intent(
        packageContext,
        cls
    )
}
