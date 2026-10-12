package com.example.okane.domain.capture

import com.example.okane.domain.Sources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Texts other than the GoPay top-up sample are synthetic. */
class CaptureEngineTest {

    private val engine = CaptureEngine()

    private fun evaluate(pkg: String, title: String?, text: String?) =
        engine.evaluate(CaptureInput(pkg, title, text, postedAt = 1_000L))

    @Test
    fun `the confirmed GoPay sample records by itself`() {
        val decision = evaluate("com.gojek.gopay", "Top up berhasil", "Top up Rp10.001 via Mandiri udah masuk.")
        assertTrue(decision is CaptureDecision.Record)
        val draft = (decision as CaptureDecision.Record).draft
        assertEquals(Sources.GOPAY, draft.source.name)
        assertEquals(10_001L, draft.amount)
        assertEquals(MoneyDirection.IN, draft.direction)
        assertEquals(Sources.LIVIN, draft.counterpartSource?.name)
        assertTrue(draft.movement)
        assertEquals("Top up via Mandiri", draft.label)
        assertEquals(KnownTemplates.GOPAY_TOP_UP.id, draft.templateId)
    }

    @Test
    fun `the same words from an unconfirmed package only suggest`() {
        val decision = evaluate("com.gojek.app", "Top up berhasil", "Top up Rp10.001 via Mandiri udah masuk.")
        assertTrue(decision is CaptureDecision.Suggest)
    }

    @Test
    fun `an unknown format from a supported app becomes a suggestion`() {
        val decision = evaluate("id.bmri.livin", "Transaksi berhasil", "Top Up GoPay Rp101.000 berhasil")
        assertTrue(decision is CaptureDecision.Suggest)
        val draft = (decision as CaptureDecision.Suggest).draft
        assertEquals(Sources.LIVIN, draft.source.name)
        assertEquals(MoneyDirection.OUT, draft.direction)
        assertEquals(Sources.GOPAY, draft.counterpartSource?.name)
        assertNull(draft.templateId)
    }

    @Test
    fun `a bank SMS is a suggestion on the SMS channel`() {
        val decision = evaluate(
            "com.google.android.apps.messaging", "BTN",
            "Dana masuk Rp2.000.000 ke rekening Anda dari PT MAJU",
        )
        val draft = (decision as CaptureDecision.Suggest).draft
        assertEquals(Sources.BALE, draft.source.name)
        assertEquals(CaptureChannel.SMS, draft.channel)
        assertEquals(MoneyDirection.IN, draft.direction)
    }

    @Test
    fun `templates never apply to relayed messages`() {
        val decision = evaluate("com.whatsapp", "GoPay", "Top up Rp10.001 via Mandiri udah masuk.")
        assertTrue(decision is CaptureDecision.Suggest)
    }

    @Test
    fun `what is not a finished transaction is ignored`() {
        assertEquals(
            CaptureDecision.Ignore(IgnoreReason.NOT_A_SOURCE),
            evaluate("com.whatsapp", "Budi", "transfer Rp50.000 ya"),
        )
        assertEquals(
            CaptureDecision.Ignore(IgnoreReason.OTP),
            evaluate("com.btpn.dc", "Jenius", "Kode OTP kamu 123456. Jangan berikan ke siapa pun."),
        )
        assertEquals(
            CaptureDecision.Ignore(IgnoreReason.PROMO),
            evaluate("com.shopeepay.id", "Promo!", "Dapatkan cashback hingga Rp20.000 pakai ShopeePay"),
        )
        assertEquals(CaptureDecision.Ignore(IgnoreReason.EMPTY), evaluate("com.gojek.gopay", "GoPay", null))
    }
}
