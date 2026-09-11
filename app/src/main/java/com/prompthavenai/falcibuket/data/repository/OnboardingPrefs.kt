package com.prompthavenai.falcibuket.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("falcibuket", Context.MODE_PRIVATE)

    val onboardingDone = MutableStateFlow(prefs.getBoolean(KEY, false))

    fun complete() {
        prefs.edit().putBoolean(KEY, true).apply()
        onboardingDone.value = true
    }

    companion object { private const val KEY = "onboarding_done" }
}
