package com.example.okane.domain.parser

import com.example.okane.data.local.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the routing contract of NotificationParser using a fake parser.
 * FakeParser is also the TEMPLATE for the real per-app parsers:
 * check it's a transaction -> extract amount -> extract merchant -> build.
 */
class NotificationParserTest {

    private class FakeParser : SourceParser {
        override val source = "FakeBank"
        override val packageNames = setOf("com.fake.bank")

        override fun parse(title: String?, text: String?): ParsedTransaction? {
            // Only success notifications count as transactions.
            if (text == null || "berhasil" !in text) return null
            val amount = AmountParser.findRupiah(text) ?: return null
            return ParsedTransaction(
                type = Transaction.TYPE_DEBIT,
                amount = amount,
                merchant = "Fake Merchant"
            )
        }
    }

    private val parser = NotificationParser(parsers = listOf(FakeParser()))

    @Test
    fun `builds a complete transaction for a matching notification`() {
        val result = parser.parse(
            packageName = "com.fake.bank",
            title = "Pembayaran berhasil",
            text = "Pembayaran Rp75.000 ke WARUNG TEST berhasil",
            postTimeMillis = 1_720_000_000_000
        )

        assertNotNull(result)
        result!!
        assertEquals("FakeBank", result.source)
        assertEquals(Transaction.TYPE_DEBIT, result.type)
        assertEquals(75_000L, result.amount)
        assertEquals("Fake Merchant", result.merchant)
        assertEquals(1_720_000_000_000, result.timestamp)
        assertEquals(0L, result.id) // not stored yet -> Room will assign
        assertTrue(result.createdAt > 0)
        // Raw text must be preserved for debugging bad parses.
        assertTrue("Rp75.000" in result.note)
    }

    @Test
    fun `returns null for an app we do not track`() {
        val result = parser.parse(
            packageName = "com.whatsapp",
            title = "New message",
            text = "Pembayaran Rp75.000 berhasil", // even with money-like text
            postTimeMillis = 0
        )
        assertNull(result)
    }

    @Test
    fun `returns null when tracked app posts a non-transaction`() {
        val result = parser.parse(
            packageName = "com.fake.bank",
            title = "Promo!",
            text = "Diskon 90% khusus hari ini!",
            postTimeMillis = 0
        )
        assertNull(result)
    }

    @Test
    fun `returns null for null text`() {
        val result = parser.parse(
            packageName = "com.fake.bank",
            title = "Pembayaran",
            text = null,
            postTimeMillis = 0
        )
        assertNull(result)
    }

    @Test
    fun `default registry routes a real GoPay notification end to end`() {
        // Uses the REAL production parser list, not fakes: proves the
        // registry wiring with the confirmed device sample.
        val result = NotificationParser().parse(
            packageName = "com.gojek.gopay",
            title = "Top up berhasil",
            text = "Top up Rp10.001 via Mandiri udah masuk.",
            postTimeMillis = 1_783_414_590_092
        )

        assertNotNull(result)
        result!!
        assertEquals("GoPay", result.source)
        assertEquals(Transaction.TYPE_KREDIT, result.type)
        assertEquals(10_001L, result.amount)
        assertEquals("Top up via Mandiri", result.merchant)
        assertEquals(1_783_414_590_092, result.timestamp)
    }
}
