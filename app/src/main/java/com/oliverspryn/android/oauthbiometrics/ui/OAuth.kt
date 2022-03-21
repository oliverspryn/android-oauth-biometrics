package com.oliverspryn.android.oauthbiometrics.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oliverspryn.android.oauthbiometrics.model.LoadingViewModel
import com.oliverspryn.android.oauthbiometrics.model.StartViewModel
import com.oliverspryn.android.oauthbiometrics.ui.account.AccountScreen
import com.oliverspryn.android.oauthbiometrics.ui.loading.LoadingScreen
import com.oliverspryn.android.oauthbiometrics.ui.start.StartScreen
import com.oliverspryn.android.oauthbiometrics.ui.theme.OAuthBiometricsTheme

@Composable
fun OAuth(
    activity: ComponentActivity
) {
    val navController = rememberNavController()

    OAuthBiometricsTheme {
        NavHost(
            navController = navController,
            startDestination = "loading"
        ) {
            composable("loading") {
                val viewModel: LoadingViewModel = hiltViewModel()

                LoadingScreen(
                    activity = activity,
                    loadingViewModel = viewModel,
                    notLoggingIn = { navController.navigate("start") },
                    loginSuccess = { navController.navigate("account") }
                )
            }

            composable("start") {
                val viewModel: StartViewModel = hiltViewModel()

                StartScreen(
                    startViewModel = viewModel
                )
            }

            composable("account") {
                AccountScreen()
            }
        }
    }
}
