package com.prompthavenai.falcibuket.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prompthavenai.falcibuket.ui.screens.*

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val CHAT = "chat"
    const val COFFEE = "coffee"
    const val TAROT = "tarot"
    const val LOVE = "love"
    const val CAREER = "career"
    const val RESULT = "result/{type}"
    const val PREMIUM = "premium"

    fun result(type: String) = "result/$type"
}

@Composable
fun FalcibuketNavHost(nav: NavHostController, startDestination: String) {
    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }
    ) {
        composable(Routes.SPLASH) { SplashScreen(nav) }
        composable(Routes.ONBOARDING) { OnboardingScreen(nav) }
        composable(Routes.MAIN) { MainScreen(nav) }
        composable(Routes.CHAT) { ChatScreen(nav) }
        composable(Routes.COFFEE) { CoffeeScreen(nav) }
        composable(Routes.TAROT) { TarotScreen(nav) }
        composable(Routes.LOVE) { LoveScreen(nav) }
        composable(Routes.CAREER) { CareerScreen(nav) }
        composable(Routes.PREMIUM) { PremiumScreen(nav) }
        composable(
            Routes.RESULT,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { entry ->
            ReadingResultScreen(
                nav,
                type = entry.arguments?.getString("type") ?: "Günlük"
            )
        }
    }
}
