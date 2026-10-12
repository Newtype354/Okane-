package com.example.okane.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every text here is SYNTHETIC: written to exercise one rule, not copied from
 * a device. None of them is a confirmed format and none may become a
 * template. Real samples belong in CaptureTemplateTest.
 */
class TransactionHeuristicsTest {

    private fun bank(text: String) = TransactionHeuristics.analyze(text, SourceKind.BANK)
    private fun wallet(text: String) = TransactionHeuristics.analyze(text, SourceKind.E_WALLET)

    @Test
    fun `one-time codes are rejected before anything else`() {
        assertEquals(Rejection.OTP, bank("Kode OTP Rp transaksi kamu 123456. Jangan berikan ke siapa pun").rejection)
        assertTrue(TransactionHeuristics.isOtp("Kode verifikasi: 884213"))
        assertFalse(TransactionHeuristics.isOtp("Transfer Rp50.000 berhasil"))
    }

    @Test
    fun `promotions are rejected unless something actually happened`() {
        assertEquals(Rejection.PROMO, wallet("Yuk top up, dapatkan cashback hingga Rp50.000!").rejection)
        val done = wallet("Pembayaran Rp20.000 berhasil. Klaim voucher berikutnya!")
        assertNull(done.rejection)
        assertEquals(20_000L, done.amount)
    }

    @Test
    fun `failed and pending payments are not transactions`() {
        assertEquals(Rejection.FAILED, bank("Transfer Rp100.000 gagal diproses").rejection)
        assertEquals(Rejection.PENDING, wallet("Menunggu pembayaran Rp75.000 untuk pesananmu").rejection)
        assertEquals(Rejection.PENDING, bank("Tagihan Rp300.000 jatuh tempo 5 Nov").rejection)
    }

    @Test
    fun `a balance is not the amount that moved`() {
        assertEquals(Rejection.NO_AMOUNT, bank("Saldo rekening kamu saat ini Rp1.250.000").rejection)
        assertEquals(Rejection.NO_AMOUNT, bank("Info: sisa saldo Rp1.250.000").rejection)

        val transfer = bank("Transfer Rp50.000 ke BUDI berhasil. Saldo akhir: Rp1.200.000")
        assertEquals(50_000L, transfer.amount)

        // Here "saldo" belongs to the movement itself.
        assertEquals(30_000L, wallet("Isi saldo Rp30.000 berhasil").amount)
    }

    @Test
    fun `a named fee is kept apart from the amount`() {
        val signals = bank("Top Up GoPay Rp100.000 berhasil. Biaya admin Rp1.000")
        assertEquals(100_000L, signals.amount)
        assertEquals(1_000L, signals.fee)
    }

    @Test
    fun `a top-up fills the wallet and drains the bank`() {
        assertEquals(MoneyDirection.IN, wallet("Top up Rp50.000 berhasil").direction)
        assertEquals(MoneyDirection.OUT, bank("Top Up GoPay Rp50.000 berhasil").direction)
        assertTrue(bank("Top Up GoPay Rp50.000 berhasil").movement)
    }

    @Test
    fun `direction follows the words`() {
        assertEquals(MoneyDirection.IN, bank("Transfer masuk Rp500.000 dari SITI").direction)
        assertEquals(MoneyDirection.IN, bank("Kamu menerima Rp75.000").direction)
        assertEquals(MoneyDirection.OUT, bank("Pembayaran QRIS Rp18.000 di KOPI TUKU berhasil").direction)
        assertEquals(MoneyDirection.OUT, wallet("Kamu membayar Rp12.500").direction)
    }

    @Test
    fun `masuk inside termasuk is not income`() {
        val signals = bank("Pembayaran Rp40.000 termasuk pajak berhasil")
        assertEquals(MoneyDirection.OUT, signals.direction)
    }

    @Test
    fun `conflicting words leave the direction to the user`() {
        assertNull(bank("Transaksi Rp10.000 tercatat").direction)
    }

    @Test
    fun `an amount without any transaction word is not enough`() {
        assertEquals(Rejection.NO_SIGNAL, wallet("Harga tiket konser Rp350.000").rejection)
    }

    @Test
    fun `counterparty is read from ke, dari and di`() {
        assertEquals("BUDI SANTOSO", bank("Transfer Rp50.000 ke BUDI SANTOSO berhasil").counterparty)
        assertEquals("SITI", bank("Transfer masuk Rp500.000 dari SITI").counterparty)
        assertEquals("KOPI TUKU", bank("Pembayaran QRIS Rp18.000 di KOPI TUKU berhasil").counterparty)
    }
}
