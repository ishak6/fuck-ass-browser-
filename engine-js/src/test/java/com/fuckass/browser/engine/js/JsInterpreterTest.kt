package com.fuckass.browser.engine.js

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsInterpreterTest {
    @Test fun evaluatesArithmeticWithPrecedence() {
        val result = JsInterpreter().execute("1 + 2 * 3")
        assertEquals(JsValue.NumberValue(7.0), result.value)
    }

    @Test fun declaresAndAssignsVariables() {
        val result = JsInterpreter().execute("let total = 2; total = total + 5; total")
        assertEquals(JsValue.NumberValue(7.0), result.value)
        assertEquals(JsValue.NumberValue(7.0), result.variables["total"])
    }

    @Test fun supportsStringsBooleansAndComparisons() {
        val result = JsInterpreter().execute("let label = 'web' + ' engine'; if (3 >= 2 && true) { label } else { 'no' }")
        assertEquals(JsValue.StringValue("web engine"), result.value)
    }

    @Test fun executesBoundedWhileLoop() {
        val result = JsInterpreter().execute("let i = 0; let sum = 0; while (i < 4) { sum = sum + i; i = i + 1; } sum")
        assertEquals(JsValue.NumberValue(6.0), result.value)
    }

    @Test fun constCannotBeReassigned() {
        try {
            JsInterpreter().execute("const locked = 1; locked = 2")
            throw AssertionError("Expected const assignment rejection")
        } catch (_: IllegalStateException) { }
    }

    @Test fun executionBudgetStopsInfiniteLoop() {
        try {
            JsInterpreter(JsLimits(maxSteps = 20)).execute("while (true) { let x = 1; }")
            throw AssertionError("Expected execution budget rejection")
        } catch (_: IllegalStateException) { }
    }

    @Test fun rejectsHostAccessAndUnsupportedSyntax() {
        try {
            JsInterpreter().execute("java.lang.Runtime")
            throw AssertionError("Expected unknown identifier")
        } catch (_: IllegalArgumentException) { }

        try {
            JsInterpreter().execute("function run() {}")
            throw AssertionError("Expected unsupported function syntax")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun sourceAndTokenLimitsAreEnforced() {
        try {
            JsInterpreter(JsLimits(maxSourceChars = 2)).execute("1 + 2")
            throw AssertionError("Expected source limit")
        } catch (_: IllegalArgumentException) { }

        try {
            JsInterpreter(JsLimits(maxTokens = 2)).execute("1 + 2")
            throw AssertionError("Expected token limit")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun reportsExecutedSteps() {
        val result = JsInterpreter().execute("let x = 1; x + 1")
        assertTrue(result.steps >= 2)
    }
}
