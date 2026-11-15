package com.fuckass.browser

import com.fuckass.browser.privacy.PrivateSiteRules

/** Central decision used before a navigation is dispatched to either profile. */
fun requiresPrivateSession(
    manualPrivateMode: Boolean,
    targetUrl: String,
    privateSiteRules: PrivateSiteRules
): Boolean = manualPrivateMode || privateSiteRules.matches(targetUrl)
