package com.example.okane.domain.capture

import com.example.okane.domain.Sources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceCatalogTest {

    @Test
    fun `the standalone GoPay app is the one confirmed package`() {
        val gopay = SourceCatalog.identify("com.gojek.gopay", "Top up berhasil")!!
        assertEquals(Sources.GOPAY, gopay.source.name)
        assertEquals(CaptureChannel.APP, gopay.channel)
        assertEquals(PackageTrust.CONFIRMED_ON_DEVICE, gopay.trust)

        // The Gojek super-app carries GoPay too, but is not confirmed.
        assertEquals(PackageTrust.UNVERIFIED, SourceCatalog.identify("com.gojek.app", null)!!.trust)
        assertEquals(PackageTrust.PUBLIC_LISTING, SourceCatalog.identify("com.shopeepay.id", null)!!.trust)
        assertEquals(PackageTrust.PUBLIC_LISTING, SourceCatalog.identify("com.btpn.dc", null)!!.trust)
    }

    @Test
    fun `an unlisted package is attributed by a hint but never trusted`() {
        val bale = SourceCatalog.identify("id.co.btn.mobile", null)!!
        assertEquals(Sources.BALE, bale.source.name)
        assertEquals(PackageTrust.UNVERIFIED, bale.trust)
        assertEquals(Sources.LIVIN, SourceCatalog.identify("com.bankmandiri.mandirionline", null)!!.source.name)
        // A short hint must be a whole segment: "livin" is not "living".
        assertNull(SourceCatalog.identify("com.living.app", null))
        // BTN's housing-estate app, not the bank.
        assertNull(SourceCatalog.identify("id.co.btn.smartresidence", null))
    }

    @Test
    fun `apps that have nothing to do with money are dropped`() {
        assertNull(SourceCatalog.identify("com.instagram.android", "Budi"))
        assertNull(SourceCatalog.identify("com.spotify.music", "Now playing"))
    }

    @Test
    fun `messaging counts only when the sender is a supported bank or wallet`() {
        val sms = SourceCatalog.identify("com.google.android.apps.messaging", "BTN")!!
        assertEquals(Sources.BALE, sms.source.name)
        assertEquals(CaptureChannel.SMS, sms.channel)
        assertEquals(PackageTrust.UNVERIFIED, sms.trust)

        val wa = SourceCatalog.identify("com.whatsapp", "Jenius")!!
        assertEquals(CaptureChannel.WHATSAPP, wa.channel)

        // A friend's chat about money is not a bank alert.
        assertNull(SourceCatalog.identify("com.whatsapp", "Budi"))
        assertNull(SourceCatalog.identify("com.google.android.apps.messaging", null))
    }

    @Test
    fun `a named counterpart is found as a whole word only`() {
        assertEquals(Sources.LIVIN, SourceCatalog.sourceMentionedIn("Top up Rp50.000 via Mandiri udah masuk.")?.name)
        assertEquals(Sources.GOPAY, SourceCatalog.sourceMentionedIn("Top Up GoPay berhasil", except = SourceCatalog.LIVIN)?.name)
        assertNull(SourceCatalog.sourceMentionedIn("Pembayaran ke WARUNGBTNX"))
        // The source itself is skipped.
        assertNull(SourceCatalog.sourceMentionedIn("Jenius: transaksi berhasil", except = SourceCatalog.JENIUS))
    }

    @Test
    fun `wallets named after the bank belong to its app`() {
        assertTrue(SourceCatalog.walletMatches(SourceCatalog.LIVIN, "Mandiri"))
        assertTrue(SourceCatalog.walletMatches(SourceCatalog.LIVIN, "Bank Mandiri"))
        assertTrue(SourceCatalog.walletMatches(SourceCatalog.BALE, "BTN"))
        assertTrue(SourceCatalog.walletMatches(SourceCatalog.JENIUS, "jenius"))
        assertFalse(SourceCatalog.walletMatches(SourceCatalog.BALE, "BCA"))
    }
}
