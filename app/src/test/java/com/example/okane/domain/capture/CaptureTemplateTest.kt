package com.example.okane.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The GoPay cases were ported from the old GoPayParserTest unchanged. */
class CaptureTemplateTest {

    private val template = KnownTemplates.GOPAY_TOP_UP

    @Test
    fun `parses the confirmed top up sample exactly as captured`() {
        // Real notification captured from the device on 2026-07-07.
        val match = template.match("Top up Rp10.001 via Mandiri udah masuk.")

        assertNotNull(match)
        match!!
        assertEquals(10_001L, match.amount)
        assertEquals("Mandiri", match.counterparty)
        assertEquals("Top up via Mandiri", match.label)
        assertEquals(MoneyDirection.IN, match.template.direction)
    }

    @Test
    fun `parses top up with different amount and channel`() {
        val match = template.match("Top up Rp250.000 via BCA udah masuk.")!!
        assertEquals(250_000L, match.amount)
        assertEquals("Top up via BCA", match.label)
    }

    @Test
    fun `tolerates spacing, case and a missing full stop`() {
        val match = template.match("  top up  Rp 75.000 via   Mandiri udah masuk ")!!
        assertEquals(75_000L, match.amount)
        assertEquals("Mandiri", match.counterparty)
    }

    @Test
    fun `rejects promo notifications`() {
        assertNull(template.match("Diskon 90% pakai GoPay, cuma hari ini!"))
    }

    @Test
    fun `rejects unconfirmed payment format even when it mentions money`() {
        assertNull(template.match("Pembayaran Rp25.000 ke WARUNG TEST berhasil"))
    }

    @Test
    fun `rejects wrong shaped top up text`() {
        assertNull(template.match("Top up Rp10.000 gagal diproses"))
        assertNull(template.match("Top up Rp10.000 via Mandiri udah masuk. Klaim voucher kamu"))
    }

    @Test
    fun `every known template matches the sample it was written from`() {
        val samples = mapOf(KnownTemplates.GOPAY_TOP_UP.id to "Top up Rp10.001 via Mandiri udah masuk.")
        KnownTemplates.ALL.forEach { known ->
            val sample = samples[known.id] ?: error("Add the real sample for ${known.id} to this test")
            assertNotNull(known.id, known.match(sample))
        }
    }
}
