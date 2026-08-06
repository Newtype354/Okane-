package com.example.okane.domain.parser

import com.example.okane.data.local.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GoPayParserTest {

    private val parser = GoPayParser()

    @Test
    fun `parses the confirmed top up sample exactly as captured`() {
        // Real notification captured from the device on 2026-07-07.
        val result = parser.parse(
            title = "Top up berhasil",
            text = "Top up Rp10.001 via Mandiri udah masuk."
        )

        assertNotNull(result)
        result!!
        assertEquals(Transaction.TYPE_KREDIT, result.type)
        assertEquals(10_001L, result.amount)
        assertEquals("Top up via Mandiri", result.merchant)
    }

    @Test
    fun `parses top up with different amount and channel`() {
        val result = parser.parse(
            title = "Top up berhasil",
            text = "Top up Rp250.000 via BCA udah masuk."
        )

        assertNotNull(result)
        result!!
        assertEquals(250_000L, result.amount)
        assertEquals("Top up via BCA", result.merchant)
    }

    @Test
    fun `rejects promo notifications`() {
        assertNull(
            parser.parse(
                title = "Promo spesial!",
                text = "Diskon 90% pakai GoPay, cuma hari ini!"
            )
        )
    }

    @Test
    fun `rejects unconfirmed payment format even when it mentions money`() {
        // Looks like a transaction, but this format was never confirmed
        // with a real sample — strictness protects the data.
        assertNull(
            parser.parse(
                title = "Pembayaran berhasil",
                text = "Pembayaran Rp25.000 ke WARUNG TEST berhasil"
            )
        )
    }

    @Test
    fun `rejects wrong shaped top up text`() {
        assertNull(parser.parse(title = "Top up", text = "Top up Rp10.000 gagal diproses"))
    }

    @Test
    fun `rejects null text`() {
        assertNull(parser.parse(title = "Top up berhasil", text = null))
    }
}
