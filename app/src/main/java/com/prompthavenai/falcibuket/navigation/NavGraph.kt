package com.prompthavenai.falcibuket.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prompthavenai.falcibuket.data.model.FortuneType
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

    const val FORTUNE_CATALOG = "fortune_catalog"
    const val FORTUNE = "fortune/{id}"
    const val FORTUNE_RESULT = "fortune_result/{id}?q={q}"

    fun result(type: String) = "result/$type"

    fun fortune(id: String) = "fortune/$id"

    fun fortuneResult(id: String, q: String = "") = "fortune_result/$id?q=${Uri.encode(q)}"

    /** Keeps the four legacy flows on their dedicated routes; everything else is generic. */
    fun openFortune(type: FortuneType): String = when (type) {
        FortuneType.COFFEE -> COFFEE
        FortuneType.TAROT -> TAROT
        FortuneType.LOVE -> LOVE
        FortuneType.CAREER_MONEY -> CAREER
        else -> fortune(type.id)
    }
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
        composable(Routes.FORTUNE_CATALOG) { FortuneCatalogScreen(nav) }
        composable(
            Routes.FORTUNE,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            FortuneFlowScreen(nav, entry.arguments?.getString("id").orEmpty())
        }
        composable(
            Routes.RESULT,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { entry ->
            ReadingResultScreen(
                nav,
                type = entry.arguments?.getString("type") ?: "Günlük"
            )
        }
        composable(
            Routes.FORTUNE_RESULT,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("q") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            ReadingResultScreen(
                nav,
                type = entry.arguments?.getString("id").orEmpty(),
                question = entry.arguments?.getString("q").orEmpty()
            )
        }
    }
}
