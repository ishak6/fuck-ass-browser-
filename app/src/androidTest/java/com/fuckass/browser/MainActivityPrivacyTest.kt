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
    fun sitePermissionPanelExplainsHttpsRequirementWithoutGrantingRuntimeAccess() {
        composeRule.onNodeWithText("Site permissions").performClick()
        composeRule.onNodeWithText("Site permission controls").fetchSemanticsNode()
        composeRule.onNodeWithText("Open an HTTPS site to edit its permissions.").assertExists()
    }

    @Test
    fun privacyPanelExposesCookieModesAndClearDataActions() {
        composeRule.onNodeWithText("Privacy & data").performClick()
        composeRule.onNodeWithText("Cookie policy: ALLOW ALL").fetchSemanticsNode()
        composeRule.onNodeWithText("Same origin").performClick()
        composeRule.onNodeWithText("Cookie policy: SAME ORIGIN ONLY").fetchSemanticsNode()
        composeRule.onNodeWithText("Clear site cookies").fetchSemanticsNode()
        composeRule.onNodeWithText("Clear all cookies").fetchSemanticsNode()
        composeRule.onNodeWithText("Clear cache").fetchSemanticsNode()
        composeRule.onNodeWithText("Clear history").fetchSemanticsNode()
    }

    @Test
    fun privateModeEnablesSecureWindowAndExitClearsIt() {
        composeRule.runOnIdle {
            assertFalse("Normal mode must not set FLAG_SECURE", isWindowSecure())
        }

        composeRule.onNodeWithText("Private: OFF").performClick()
        composeRule.runOnIdle {
            assertTrue("Entering private mode must set FLAG_SECURE", isWindowSecure())
        }

        composeRule.onNodeWithText("Private: ON").performClick()
        composeRule.runOnIdle {
            assertFalse("Leaving private mode must clear FLAG_SECURE", isWindowSecure())
        }
    }
}
