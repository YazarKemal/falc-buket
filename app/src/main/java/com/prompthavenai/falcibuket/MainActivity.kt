package com.prompthavenai.falcibuket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.prompthavenai.falcibuket.data.repository.OnboardingPrefs
import com.prompthavenai.falcibuket.navigation.FalcibuketNavHost
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.theme.FalcibuketTheme
import com.prompthavenai.falcibuket.ui.theme.NightBg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = OnboardingPrefs(this)
        val start = if (prefs.onboardingDone.value) Routes.MAIN else Routes.SPLASH
        setContent {
            FalcibuketTheme {
                Surface(Modifier.fillMaxSize().background(NightBg)) {
                    val nav = rememberNavController()
                    FalcibuketNavHost(nav, start)
                }
            }
        }
    }
}
