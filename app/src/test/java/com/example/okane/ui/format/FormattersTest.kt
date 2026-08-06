package com.example.okane.ui.format

import com.example.okane.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `formats with dot as thousands separator`() {
        assertEquals("Rp50.000", Formatters.rupiah(50_000))
        assertEquals("Rp1.234.567", Formatters.rupiah(1_234_567))
    }

    @Test
    fun `zero has no separator`() {
        assertEquals("Rp0", Formatters.rupiah(0))
    }

    @Test
    fun `negative balance puts sign before rupiah`() {
        assertEquals("-Rp50.000", Formatters.balanceRupiah(-50_000))
        assertEquals("Rp50.000", Formatters.balanceRupiah(50_000))
    }

    @Test
    fun `income gets plus sign`() {
        assertEquals(
            "+Rp100.000",
            Formatters.signedRupiah(100_000, TransactionType.IN)
        )
    }

    @Test
    fun `expense gets minus sign`() {
        assertEquals(
            "-Rp75.000",
            Formatters.signedRupiah(75_000, TransactionType.OUT)
        )
    }

    @Test
    fun `transfer gets no sign because it is neither a gain nor a loss`() {
        assertEquals(
            "Rp250.000",
            Formatters.signedRupiah(250_000, TransactionType.TRANSFER)
        )
    }
}
