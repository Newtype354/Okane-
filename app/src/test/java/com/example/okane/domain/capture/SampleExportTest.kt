package com.example.okane.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleExportTest {

    @Test
    fun `account and reference numbers are masked, amounts kept`() {
        val masked = SampleExport.mask("Transfer Rp1500000 ke rek 1234567890 berhasil. Ref 99887766")
        assertEquals("Transfer Rp1500000 ke rek ********** berhasil. Ref ********", masked)
    }

    @Test
    fun `the name after a n is masked`() {
        val masked = SampleExport.mask("Transfer Rp50.000 ke BCA a.n. BUDI SANTOSO berhasil")
        assertFalse(masked.contains("BUDI"))
        assertTrue(masked.contains("a.n. ***"))
        assertTrue(masked.contains("Rp50.000"))
    }

    @Test
    fun `short numbers and the export header survive`() {
        val text = SampleExport.format(
            listOf(CapturedSample("com.gojek.gopay", "Top up berhasil", "Top up Rp10.001 via Mandiri udah masuk.", 42L))
        )
        assertTrue(text.startsWith("Sampel notifikasi Okane (1)"))
        assertTrue(text.contains("com.gojek.gopay @ 42"))
        assertTrue(text.contains("Top up Rp10.001 via Mandiri udah masuk."))
    }

    @Test
    fun `battery tips exist for the aggressive skins only`() {
        assertTrue(BatteryGuide.tipFor("Xiaomi")!!.contains("Mulai otomatis"))
        assertTrue(BatteryGuide.tipFor("samsung") != null)
        assertNull(BatteryGuide.tipFor("Google"))
    }
}
