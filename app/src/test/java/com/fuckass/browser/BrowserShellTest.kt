package com.fuckass.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserShellTest {
    @Test fun initialVersionIsDevelopmentVersion() {
        assertEquals("0.1.0", BuildConfig.VERSION_NAME)
    }
}
