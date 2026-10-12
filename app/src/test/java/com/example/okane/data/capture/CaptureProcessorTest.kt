package com.example.okane.data.capture

import com.example.okane.data.local.SuggestionStatus
import com.example.okane.data.local.TransactionEntity
import com.example.okane.data.local.WalletEntity
import com.example.okane.domain.Sources
import com.example.okane.domain.capture.CaptureInput
import com.example.okane.domain.capture.CaptureRules
import com.example.okane.domain.capture.IgnoreReason
import com.example.okane.domain.capture.MoneyDirection
import com.example.okane.domain.model.TransactionSource
import com.example.okane.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The GoPay text is the confirmed device sample; the Livin and BTN texts are
 * synthetic stand-ins for formats not collected yet.
 */
class CaptureProcessorTest {

    private val tunai = WalletEntity(1, "Tunai", isDefault = true)
    private val gopay = WalletEntity(2, "GoPay", sortOrder = 1)
    private val mandiri = WalletEntity(3, "Mandiri", sortOrder = 2)
    private val btn = WalletEntity(4, "BTN", sortOrder = 3)

    private val t0 = 1_760_000_000_000L
    private val minute = CaptureRules.MINUTE

    private val goPayTopUp = CaptureInput(
        "com.gojek.gopay", "Top up berhasil", "Top up Rp100.000 via Mandiri udah masuk.", t0,
    )
    private val livinTopUp = CaptureInput(
        "id.bmri.livin", "Transaksi berhasil", "Top Up GoPay Rp101.000 berhasil", t0 - minute,
    )

    private var toggles = mutableMapOf<String, Boolean>()

    private fun processor(store: FakeCaptureStore) = CaptureProcessor(
        store = store,
        toggles = object : SourceToggles {
            override fun isEnabled(source: String) = toggles[source] ?: true
        },
        clock = { t0 + 5 * minute },
    )

    @Test
    fun `a confirmed top-up without a bank wallet is income in the GoPay wallet`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay)
        val outcome = processor(store).process(goPayTopUp)

        assertTrue(outcome is CaptureOutcome.Recorded)
        val row = store.transactions.single()
        assertEquals(TransactionType.IN, row.type)
        assertEquals(gopay.id, row.walletId)
        assertEquals(100_000L, row.amount)
        assertEquals(TransactionSource.NOTIFICATION, row.source)
        assertEquals(Sources.GOPAY, row.sourceApp)
        assertEquals("Top up via Mandiri", row.merchant)
        assertEquals(t0, row.occurredAt)
    }

    @Test
    fun `a confirmed top-up from the user's own bank is a transfer, not income`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        processor(store).process(goPayTopUp)

        val row = store.transactions.single()
        assertEquals(TransactionType.TRANSFER, row.type)
        assertEquals(mandiri.id, row.walletId)
        assertEquals(gopay.id, row.toWalletId)
        assertEquals(-100_000L, store.balance(mandiri.id))
        assertEquals(100_000L, store.balance(gopay.id))
    }

    @Test
    fun `the same notification posted again is dropped`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay)
        val processor = processor(store)
        processor.process(goPayTopUp)
        val again = processor.process(goPayTopUp.copy(postedAt = t0 + 2 * minute))

        assertEquals(CaptureOutcome.Repeat, again)
        assertEquals(1, store.transactions.size)
    }

    @Test
    fun `a source switched off is not read`() = runBlocking {
        toggles[Sources.GOPAY] = false
        val store = FakeCaptureStore(tunai, gopay)
        assertEquals(CaptureOutcome.Disabled, processor(store).process(goPayTopUp))
        assertTrue(store.transactions.isEmpty())
    }

    @Test
    fun `apps that are not finance sources are ignored`() = runBlocking {
        val store = FakeCaptureStore(tunai)
        val outcome = processor(store).process(CaptureInput("com.whatsapp", "Budi", "transfer Rp50.000 ya", t0))
        assertEquals(CaptureOutcome.Ignored(IgnoreReason.NOT_A_SOURCE), outcome)
    }

    @Test
    fun `money that may already be recorded by hand waits for confirmation`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay)
        store.insertTransaction(
            TransactionEntity(
                amount = 100_000, type = TransactionType.IN, walletId = gopay.id,
                occurredAt = t0 - 3 * minute, createdAt = t0, source = TransactionSource.MANUAL,
            )
        )

        val outcome = processor(store).process(goPayTopUp)

        assertTrue(outcome is CaptureOutcome.Suggested)
        assertEquals(1, store.transactions.size)
        val suggestion = store.suggestions.single()
        assertEquals(store.transactions.single().id, suggestion.duplicateOfId)
        assertEquals(TransactionType.IN, suggestion.type)
    }

    @Test
    fun `an unconfirmed bank format is suggested as a transfer when both wallets exist`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        val outcome = processor(store).process(livinTopUp)

        assertTrue(outcome is CaptureOutcome.Suggested)
        assertTrue(store.transactions.isEmpty())
        val suggestion = store.suggestions.single()
        assertEquals(Sources.LIVIN, suggestion.sourceApp)
        assertEquals(TransactionType.TRANSFER, suggestion.type)
        assertEquals(MoneyDirection.OUT, suggestion.direction)
        assertEquals(mandiri.id, suggestion.walletId)
        assertEquals(gopay.id, suggestion.toWalletId)
    }

    @Test
    fun `confirming the bank's half of a recorded transfer only adds the fee`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        val processor = processor(store)
        processor.process(livinTopUp)          // suggestion, Rp101.000 from the bank
        processor.process(goPayTopUp)          // transfer, Rp100.000 arrived

        val transfer = store.transactions.single { it.type == TransactionType.TRANSFER }
        val suggestion = store.suggestions.single()
        assertEquals("the pending half is flagged", transfer.id, suggestion.duplicateOfId)

        val result = processor.accept(
            suggestion.id,
            Confirmation(TransactionType.TRANSFER, 101_000, walletId = mandiri.id, toWalletId = gopay.id),
        )

        assertEquals(AcceptResult.Recorded(transfer.id), result)
        assertEquals(1, store.transactions.count { it.type == TransactionType.TRANSFER })
        val fee = store.transactions.single { it.type == TransactionType.OUT }
        assertEquals(1_000L, fee.amount)
        assertEquals(CaptureRules.FEE_LABEL, fee.merchant)
        assertEquals(-101_000L, store.balance(mandiri.id))
        assertEquals(100_000L, store.balance(gopay.id))
        assertEquals(SuggestionStatus.ACCEPTED, store.suggestions.single().status)
    }

    @Test
    fun `a half recorded as spending is turned into the transfer it was`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        val processor = processor(store)
        processor.process(livinTopUp)
        // The user confirmed the bank's notification as plain spending first.
        processor.accept(store.suggestions.single().id, Confirmation(TransactionType.OUT, 101_000, mandiri.id))

        val outcome = processor.process(goPayTopUp)

        assertEquals(true, (outcome as CaptureOutcome.Recorded).merged)
        val transfer = store.transactions.single { it.type == TransactionType.TRANSFER }
        assertEquals(100_000L, transfer.amount)
        assertEquals(mandiri.id, transfer.walletId)
        assertEquals(gopay.id, transfer.toWalletId)
        assertEquals(1_000L, store.transactions.single { it.type == TransactionType.OUT }.amount)
        assertEquals(-101_000L, store.balance(mandiri.id))
        assertEquals(100_000L, store.balance(gopay.id))
    }

    @Test
    fun `a hand-typed half is never rewritten, only pointed at`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        val manualId = store.insertTransaction(
            TransactionEntity(
                amount = 100_000, type = TransactionType.OUT, walletId = mandiri.id,
                occurredAt = t0 - minute, createdAt = t0, source = TransactionSource.MANUAL,
            )
        )

        val outcome = processor(store).process(goPayTopUp)

        assertTrue(outcome is CaptureOutcome.Suggested)
        assertEquals(TransactionType.OUT, store.transactions.single().type)
        assertEquals(manualId, store.suggestions.single().duplicateOfId)
    }

    @Test
    fun `confirming a suggestion records it once`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, btn)
        val processor = processor(store)
        processor.process(
            CaptureInput("com.google.android.apps.messaging", "BTN", "Dana masuk Rp2.000.000 dari PT MAJU", t0)
        )
        val suggestion = store.suggestions.single()
        assertEquals(btn.id, suggestion.walletId)
        assertEquals(TransactionType.IN, suggestion.type)

        val first = processor.accept(suggestion.id, Confirmation(TransactionType.IN, 2_000_000, btn.id))
        val second = processor.accept(suggestion.id, Confirmation(TransactionType.IN, 2_000_000, btn.id))

        assertTrue(first is AcceptResult.Recorded)
        assertEquals(AcceptResult.NotPending, second)
        val row = store.transactions.single()
        assertEquals(Sources.BALE, row.sourceApp)
        assertEquals(t0, row.occurredAt)
        assertNull(row.categoryId)
    }

    @Test
    fun `a transfer needs two different wallets`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        val processor = processor(store)
        processor.process(livinTopUp)
        val result = processor.accept(
            store.suggestions.single().id,
            Confirmation(TransactionType.TRANSFER, 101_000, walletId = mandiri.id, toWalletId = mandiri.id),
        )
        assertTrue(result is AcceptResult.Invalid)
        assertTrue(store.transactions.isEmpty())
    }

    @Test
    fun `dismissing keeps the text but records nothing`() = runBlocking {
        val store = FakeCaptureStore(tunai, gopay, mandiri)
        val processor = processor(store)
        processor.process(livinTopUp)
        assertTrue(processor.dismiss(store.suggestions.single().id))

        assertEquals(SuggestionStatus.DISMISSED, store.suggestions.single().status)
        assertNotNull(store.suggestions.single().text)
        assertTrue(store.transactions.isEmpty())
        // And the same notification does not come back.
        assertEquals(CaptureOutcome.Repeat, processor.process(livinTopUp))
    }
}
