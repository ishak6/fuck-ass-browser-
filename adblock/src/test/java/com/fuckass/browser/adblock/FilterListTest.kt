package com.fuckass.browser.adblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListTest {
    @Test fun blocksDomainAndSubdomains() {
        val list = FilterList.parse("||ads.example^")
        assertTrue(list.shouldBlock("https://ads.example/banner.js", "script", "https://news.example"))
        assertTrue(list.shouldBlock("https://sub.ads.example/a", "image", "https://news.example"))
        assertFalse(list.shouldBlock("https://notads.example/a", "image", "https://news.example"))
    }

    @Test fun exceptionOverridesMatchingBlock() {
        val list = FilterList.parse("||ads.example^\n@@||ads.example/allowed")
        assertFalse(list.shouldBlock("https://ads.example/allowed", "image", "https://news.example"))
        assertTrue(list.shouldBlock("https://ads.example/blocked", "image", "https://news.example"))
    }

    @Test fun resourceTypeAndDomainOptionsAreApplied() {
        val list = FilterList.parse("||cdn.example^\$script,domain=news.example")
        assertTrue(list.shouldBlock("https://cdn.example/a.js", "script", "https://news.example"))
        assertFalse(list.shouldBlock("https://cdn.example/a.png", "image", "https://news.example"))
        assertFalse(list.shouldBlock("https://cdn.example/a.js", "script", "https://other.example"))
    }

    @Test fun thirdPartyOptionRequiresTopLevelOrigin() {
        val list = FilterList.parse("||tracker.example^\$third-party")
        assertTrue(list.shouldBlock("https://tracker.example/pixel", "image", "https://publisher.example"))
        assertFalse(list.shouldBlock("https://tracker.example/pixel", "image", "https://tracker.example"))
        assertFalse(list.shouldBlock("https://tracker.example/pixel", "image", null))
    }

    @Test fun unsupportedSyntaxIsIgnored() {
        val list = FilterList.parse("||ads.example^\$script,important\n/regex/")
        assertFalse(list.shouldBlock("https://ads.example/a.js", "script", "https://news.example"))
    }

    @Test fun parserRejectsWildcardPathAndBoundsInput() {
        val list = FilterList.parse("||ads.example/a*")
        assertFalse(list.shouldBlock("https://ads.example/anything", "image", "https://news.example"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedInput() {
        FilterList.parse("x".repeat(5 * 1024 * 1024 + 1))
    }

    @Test fun malformedAndOversizedHostRulesAreIgnored() {
        val list = FilterList.parse("||../bad^\n||ads.example^")
        assertTrue(list.shouldBlock("https://ads.example/a", "image", "https://news.example"))
    }
}
