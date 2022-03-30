package com.oliverspryn.android.oauthbiometrics.ui

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oliverspryn.android.oauthbiometrics.model.AccountViewModel
import com.oliverspryn.android.oauthbiometrics.model.LoadingViewModel
import com.oliverspryn.android.oauthbiometrics.model.StartViewModel
import com.oliverspryn.android.oauthbiometrics.ui.theme.OAuthBiometricsTheme
import com.oliverspryn.android.oauthbiometrics.utils.ext.navigateAndClearBackStack

@Composable
fun OAuth(
    activity: FragmentActivity
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
                    onNotLoggingIn = { navController.navigateAndClearBackStack("start") },
                    onLoginSuccess = { navController.navigateAndClearBackStack("account") }
                )
            }

            composable("start") {
                val viewModel: StartViewModel = hiltViewModel()

                StartScreen(
                    activity = activity,
                    startViewModel = viewModel,
                    onLoginSuccess = { navController.navigateAndClearBackStack("account") }
                )
            }

            composable("account") {
                val viewModel: AccountViewModel = hiltViewModel()

                AccountScreen(
                    accountViewModel = viewModel,
                    activity = activity
                )
            }
        }
    }
}
