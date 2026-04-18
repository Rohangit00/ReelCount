package com.reelcount.app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.*
import com.reelcount.app.ui.dashboard.DashboardScreen
import com.reelcount.app.ui.onboarding.OnboardingScreen
import com.reelcount.app.ui.settings.SettingsScreen
import com.reelcount.app.ui.theme.ReelCountTheme
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasCompletedOnboarding = getPreferences(Context.MODE_PRIVATE)
            .getBoolean("onboarding_complete", false)

        setContent {
            ReelCountTheme {
                val navController = rememberNavController()
                val startDestination = if (hasCompletedOnboarding) Routes.DASHBOARD else Routes.ONBOARDING

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            onComplete = {
                                getPreferences(Context.MODE_PRIVATE)
                                    .edit().putBoolean("onboarding_complete", true).apply()
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            onNavigateToSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
