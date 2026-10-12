package com.example.okane.ui.capture

import com.example.okane.data.local.CaptureSuggestionEntity
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.capture.CaptureChannel
import com.example.okane.domain.capture.MoneyDirection
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureInboxPresenterTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = LocalDateTime.of(2026, 10, 12, 9, 0).atZone(zone).toInstant()
    private val at = LocalDateTime.of(2026, 10, 12, 8, 10).atZone(zone).toInstant().toEpochMilli()

    private val wallets = listOf(
        WalletEntity(1, "Tunai", isDefault = true),
        WalletEntity(2, "GoPay", sortOrder = 1),
        WalletEntity(3, "Mandiri", sortOrder = 2),
        WalletEntity(4, "Lama", sortOrder = 3, isArchived = true),
    )

    private fun suggestion(
        id: Long,
        type: TransactionType? = TransactionType.OUT,
        walletId: Long? = 3,
        toWalletId: Long? = null,
        duplicateOfId: Long? = null,
        channel: CaptureChannel = CaptureChannel.APP,
        postedAt: Long = at,
    ) = CaptureSuggestionEntity(
        id = id, sourceApp = "Livin", packageName = "id.bmri.livin", channel = channel,
        title = "Transaksi berhasil", text = "Top Up GoPay Rp101.000 berhasil", amount = 101_000,
        direction = MoneyDirection.OUT, type = type, walletId = walletId, toWalletId = toWalletId,
        counterparty = null, postedAt = postedAt, createdAt = postedAt, duplicateOfId = duplicateOfId,
    )

    private fun cards(suggestions: List<CaptureSuggestionEntity>, rows: List<TransactionEntity> = emptyList()) =
        CaptureInboxPresenter.cards(suggestions, wallets, rows, now, zone)

    @Test
    fun `cards are newest first with a readable time and channel`() {
        val result = cards(listOf(suggestion(1, postedAt = at - 60_000), suggestion(2, channel = CaptureChannel.SMS)))
        assertEquals(listOf(2L, 1L), result.map { it.id })
        assertEquals("SMS", result[0].channelLabel)
        assertNull(result[1].channelLabel)
        assertEquals("Hari ini · 08.10", result[0].timeLabel)
    }

    @Test
    fun `the default wallet fills in when no wallet was guessed`() {
        val card = cards(listOf(suggestion(1, type = null, walletId = null))).single()
        val selection = CaptureInboxPresenter.initialSelection(card, wallets)
        assertEquals(1L, selection.walletId)
        assertNull(selection.type)
        assertFalse(selection.complete)
    }

    @Test
    fun `a transfer is complete only with two different wallets`() {
        assertFalse(ReviewSelection(TransactionType.TRANSFER, 3, null).complete)
        assertFalse(ReviewSelection(TransactionType.TRANSFER, 3, 3).complete)
        assertTrue(ReviewSelection(TransactionType.TRANSFER, 3, 2).complete)
        assertTrue(ReviewSelection(TransactionType.OUT, 3, null).complete)
    }

    @Test
    fun `archived wallets are not offered`() {
        assertEquals(listOf("Tunai", "GoPay", "Mandiri"), CaptureInboxPresenter.choices(wallets).map { it.name })
    }

    @Test
    fun `duplicate notes say what is already there`() {
        val transfer = TransactionEntity(
            id = 10, amount = 100_000, type = TransactionType.TRANSFER, walletId = 3, toWalletId = 2,
            occurredAt = at, createdAt = at, source = TransactionSource.NOTIFICATION, sourceApp = "GoPay",
        )
        val manual = TransactionEntity(
            id = 11, amount = 101_000, type = TransactionType.OUT, walletId = 3,
            occurredAt = at, createdAt = at, source = TransactionSource.MANUAL,
        )
        val notes = cards(
            listOf(
                suggestion(1, type = TransactionType.TRANSFER, toWalletId = 2, duplicateOfId = 10),
                suggestion(2, duplicateOfId = 11, postedAt = at - 1),
                // Proposed as spending, while the money is already a transfer.
                suggestion(3, duplicateOfId = 10, postedAt = at - 2),
            ),
            listOf(transfer, manual),
        ).map { it.duplicateNote!! }

        assertEquals(
            "Pindah saldo Mandiri → GoPay Rp100.000 sudah tercatat dari GoPay. " +
                "Catat untuk melengkapinya: selisih Rp1.000 dicatat sebagai biaya admin.",
            notes[0],
        )
        assertTrue(notes[1], notes[1].contains("yang kamu catat sendiri"))
        assertTrue(notes[2], notes[2].startsWith("Sudah tercatat sebagai pindah saldo Mandiri → GoPay"))
    }
}
