package com.oliverspryn.android.oauthbiometrics.utils.ext

import androidx.navigation.NavHostController

fun NavHostController.navigateAndClearBackStack(
    routeToNavigate: String
) {
    navigate(routeToNavigate) {
        val previousRouteToClear = currentBackStackEntry?.destination?.route ?: return@navigate

        popUpTo(previousRouteToClear) {
            inclusive = true
        }
    }
}
