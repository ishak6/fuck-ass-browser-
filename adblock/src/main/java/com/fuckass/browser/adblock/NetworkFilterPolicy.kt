package com.fuckass.browser.adblock

/** Small, engine-independent request decision API. Rule parsing lives in :adblock. */
fun interface NetworkFilterPolicy {
    fun shouldBlock(url: String, resourceType: String, topLevelUrl: String?): Boolean
}

object AllowAllNetworkFilterPolicy : NetworkFilterPolicy {
    override fun shouldBlock(url: String, resourceType: String, topLevelUrl: String?): Boolean = false
}
