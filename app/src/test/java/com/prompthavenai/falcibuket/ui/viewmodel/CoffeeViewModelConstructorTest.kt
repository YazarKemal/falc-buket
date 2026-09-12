package com.prompthavenai.falcibuket.ui.viewmodel

import android.app.Application
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression guard: CoffeeViewModel is an AndroidViewModel created by the
 * default Compose `viewModel()` factory, which requires a public
 * `(Application)` JVM constructor. Without `@JvmOverloads` the default
 * parameter removed that overload and opening the Coffee screen crashed with
 * NoSuchMethodException.
 */
class CoffeeViewModelConstructorTest {

    @Test
    fun `exposes an Application constructor for the default viewModel factory`() {
        val constructor = CoffeeViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }
}
