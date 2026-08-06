package com.example.okane.ui.wallets

import com.example.okane.data.local.WalletBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletsPresenterTest {

    @Test
    fun `total includes active and archived balances`() {
        val rows = listOf(
            wallet(id = 1, balance = 500_000),
            wallet(id = 2, balance = -50_000),
            wallet(id = 3, balance = 125_000, archived = true),
        )

        assertEquals(575_000L, WalletsPresenter.total(rows))
        assertEquals(listOf(1L, 2L), WalletsPresenter.active(rows).map { it.walletId })
        assertEquals(listOf(3L), WalletsPresenter.archived(rows).map { it.walletId })
        assertTrue(WalletsPresenter.archivedContributes(rows))
    }

    @Test
    fun `archived note is hidden when archived balance is zero`() {
        val rows = listOf(
            wallet(id = 1, balance = 100_000),
            wallet(id = 2, balance = 0, archived = true),
        )

        assertFalse(WalletsPresenter.archivedContributes(rows))
    }

    @Test
    fun `rupiah input is non-negative whole and limited like keypad`() {
        assertEquals("50000", sanitizeWalletAmountInput("Rp50.000"))
        assertEquals("123456789012", sanitizeWalletAmountInput("-123456789012999"))
    }

    private fun wallet(
        id: Long,
        balance: Long,
        archived: Boolean = false,
    ) = WalletBalance(
        walletId = id,
        name = "Wallet $id",
        balance = balance,
        isDefault = id == 1L,
        initialBalance = 0,
        monthlyFee = 0,
        isArchived = archived,
    )
}

