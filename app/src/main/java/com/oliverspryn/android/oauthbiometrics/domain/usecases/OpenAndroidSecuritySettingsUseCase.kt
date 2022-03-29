package com.oliverspryn.android.oauthbiometrics.domain.usecases

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.oliverspryn.android.oauthbiometrics.di.factories.IntentFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class OpenAndroidSecuritySettingsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: IntentFactory
) {

    operator fun invoke() {
        val settingsIntent = intentFactory.newInstance(Settings.ACTION_SECURITY_SETTINGS)
        settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        context.startActivity(settingsIntent) // Don't care about the outcome, will re-evaluate on resume
    }
}