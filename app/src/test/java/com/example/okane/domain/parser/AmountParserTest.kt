package com.example.okane.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    // --- findRupiah: locating the amount inside real sentence shapes ---

    @Test
    fun `finds Indonesian format amount in sentence`() {
        assertEquals(
            50_000L,
            AmountParser.findRupiah("Pembayaran Rp50.000 ke WARUNG BU SITI berhasil")
        )
    }

    @Test
    fun `finds amount with space after Rp`() {
        assertEquals(1_234_567L, AmountParser.findRupiah("Transfer Rp 1.234.567 terkirim"))
    }

    @Test
    fun `finds amount with Rp dot prefix`() {
        assertEquals(25_000L, AmountParser.findRupiah("Top up Rp. 25.000 berhasil"))
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(10_000L, AmountParser.findRupiah("bayar rp10.000 sukses"))
    }

    @Test
    fun `finds IDR english format`() {
        assertEquals(50_000L, AmountParser.findRupiah("You paid IDR 50,000.00"))
    }

    @Test
    fun `drops decimal part in Indonesian format`() {
        assertEquals(100_000L, AmountParser.findRupiah("Top up Rp100.000,00 berhasil"))
    }

    @Test
    fun `ignores sentence-ending period`() {
        assertEquals(50_000L, AmountParser.findRupiah("Kamu membayar sebesar Rp50.000."))
    }

    @Test
    fun `returns first amount when several are present`() {
        assertEquals(
            50_000L,
            AmountParser.findRupiah("Pembayaran Rp50.000 berhasil. Sisa saldo Rp200.000")
        )
    }

    @Test
    fun `returns null when no amount present`() {
        assertNull(AmountParser.findRupiah("Kode OTP kamu adalah 123456"))
    }

    @Test
    fun `does not match rp inside a word`() {
        assertNull(AmountParser.findRupiah("kuota terpakai 50.000"))
    }

    // --- parseNumber: separator disambiguation rules ---

    @Test
    fun `dot as thousands`() {
        assertEquals(50_000L, AmountParser.parseNumber("50.000"))
    }

    @Test
    fun `comma as thousands`() {
        assertEquals(50_000L, AmountParser.parseNumber("50,000"))
    }

    @Test
    fun `repeated dots are thousands`() {
        assertEquals(1_234_567L, AmountParser.parseNumber("1.234.567"))
    }

    @Test
    fun `two digits after single dot is a decimal`() {
        assertEquals(500L, AmountParser.parseNumber("500.00"))
    }

    @Test
    fun `two digits after single comma is a decimal`() {
        assertEquals(500L, AmountParser.parseNumber("500,00"))
    }

    @Test
    fun `mixed separators - comma decimal`() {
        assertEquals(1_234_567L, AmountParser.parseNumber("1.234.567,89"))
    }

    @Test
    fun `mixed separators - dot decimal`() {
        assertEquals(1_234_567L, AmountParser.parseNumber("1,234,567.89"))
    }

    @Test
    fun `plain digits`() {
        assertEquals(10_000L, AmountParser.parseNumber("10000"))
    }

    @Test
    fun `empty string is null`() {
        assertNull(AmountParser.parseNumber(""))
    }

    @Test
    fun `non numeric garbage is null`() {
        assertNull(AmountParser.parseNumber("12a34"))
    }

    // --- findAllRupiah: every amount, with its position ---

    @Test
    fun `finds every amount in reading order with positions`() {
        val text = "Transfer Rp50.000 berhasil. Sisa saldo Rp1.200.000"
        val all = AmountParser.findAllRupiah(text)
        assertEquals(listOf(50_000L, 1_200_000L), all.map { it.amount })
        assertEquals("Rp50.000", text.substring(all[0].range.first, all[0].range.last + 1))
        assertEquals(text.indexOf("Rp1.200.000"), all[1].range.first)
    }

    @Test
    fun `findAll is empty without amounts`() {
        assertEquals(emptyList<RupiahMatch>(), AmountParser.findAllRupiah("Kode OTP 123456"))
    }
}
