package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oliverspryn.android.oauthbiometrics.model.StartViewModel
import com.oliverspryn.android.oauthbiometrics.ui.start.StartScreen
import com.oliverspryn.android.oauthbiometrics.ui.theme.OAuthBiometricsTheme

@Composable
fun OAuth() {
    val navController = rememberNavController()

    OAuthBiometricsTheme {
        NavHost(
            navController = navController,
            startDestination = "start"
        ) {
            composable("start") {
                val viewModel: StartViewModel = hiltViewModel()

                StartScreen(
                    startViewModel = viewModel,
                    onReauthTap = {}
                )
            }
        }
    }
}
