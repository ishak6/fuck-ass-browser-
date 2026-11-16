package com.fuckass.browser

import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPrivacyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun isWindowSecure(): Boolean =
        composeRule.activity.window.attributes.flags and
            WindowManager.LayoutParams.FLAG_SECURE != 0

    @Test
    fun privateModeEnablesSecureWindowAndExitClearsIt() {
        composeRule.waitUntil(timeoutMillis = 5_000) { !isWindowSecure() }
        assertFalse("Normal mode must not set FLAG_SECURE", isWindowSecure())

        composeRule.onNodeWithText("Private: OFF").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { isWindowSecure() }
        assertTrue("Private mode must set FLAG_SECURE", isWindowSecure())

        composeRule.onNodeWithText("Private: ON").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { !isWindowSecure() }
        assertFalse("Leaving private mode must clear FLAG_SECURE", isWindowSecure())
    }
}
