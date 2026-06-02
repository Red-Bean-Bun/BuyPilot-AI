package com.buypilot.feature.chat.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CriteriaBudgetOptionsTest {
    @Test
    fun digitalBudgetContextUsesPhoneScale() {
        val options = budgetSliderOptions(currentBudget = 200, productContext = "数码电子 智能手机")

        assertFalse(options.contains(200))
        assertTrue(options.first() >= 800)
        assertEquals(800, 200.nearestBudgetOption(options))
        assertEquals(3000, defaultBudgetPreset("智能手机"))
    }

    @Test
    fun nonDigitalBudgetContextKeepsLightScale() {
        val options = budgetSliderOptions(currentBudget = 200, productContext = "美妆护肤 洗面奶")

        assertTrue(options.contains(200))
        assertEquals(200, defaultBudgetPreset("美妆护肤"))
    }
}
