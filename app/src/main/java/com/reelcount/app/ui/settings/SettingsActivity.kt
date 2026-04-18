package com.reelcount.app.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reelcount.app.ui.theme.ReelCountTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Standalone SettingsActivity referenced by the Accessibility Service config.
 * This allows Android to show a "Settings" button in the Accessibility service detail page.
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelCountTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
