package com.example.okane.ui.dashboard

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountSizeTest {

    @Test
    fun `ordinary totals use the full size`() {
        assertEquals(44.sp, amountSize(0))
        assertEquals(44.sp, amountSize(10_133_000))
        assertEquals(44.sp, amountSize(999_999_999))
    }

    @Test
    fun `billions shrink so the total stays on one line`() {
        assertEquals(38.sp, amountSize(1_250_000_000))
        assertEquals(32.sp, amountSize(125_000_000_000))
        // The minus sign counts as a character too.
        assertEquals(32.sp, amountSize(-1_250_000_000))
    }
}
