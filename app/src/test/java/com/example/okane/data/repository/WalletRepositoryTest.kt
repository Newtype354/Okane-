package com.example.okane.data.repository

import com.example.okane.data.local.WalletBalance
import com.example.okane.data.local.WalletDao
import com.example.okane.data.local.WalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRepositoryTest {

    @Test
    fun `add trims name makes first default and monthly fee stays metadata`() = runBlocking {
        val dao = FakeWalletDao()
        val repository = WalletRepository(dao)

        val id = repository.create(
            name = "  BCA  ",
            initialBalance = 2_000_000,
            monthlyFee = 15_000,
        )

        val saved = dao.findById(id)!!
        assertEquals("BCA", saved.name)
        assertEquals(2_000_000L, saved.initialBalance)
        assertEquals(15_000L, saved.monthlyFee)
        assertTrue(saved.isDefault)
        assertEquals(0, dao.transactionWrites)
    }

    @Test
    fun `add into defensive empty active state becomes default`() = runBlocking {
        val dao = FakeWalletDao(
            WalletEntity(id = 1, name = "Lama", isDefault = true, isArchived = true),
        )
        val repository = WalletRepository(dao)

        val id = repository.create("Tunai")

        assertTrue(dao.findById(id)!!.isDefault)
        assertFalse(dao.findById(1)!!.isDefault)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `add rejects blank name`() {
        runBlocking {
            WalletRepository(FakeWalletDao()).create("   ")
        }
    }

    @Test
    fun `setting default moves the only default flag`() = runBlocking {
        val dao = FakeWalletDao(
            WalletEntity(id = 1, name = "Tunai", isDefault = true),
            WalletEntity(id = 2, name = "BCA", sortOrder = 1),
        )
        val repository = WalletRepository(dao)

        assertTrue(repository.setDefault(2))

        assertFalse(dao.findById(1)!!.isDefault)
        assertTrue(dao.findById(2)!!.isDefault)
    }

    @Test
    fun `editing initial balance preserves wallet id and stored metadata`() = runBlocking {
        val dao = FakeWalletDao(
            WalletEntity(id = 7, name = "BCA", initialBalance = 10_000, monthlyFee = 5_000),
        )
        val repository = WalletRepository(dao)

        assertTrue(repository.updateDetails(7, " BCA Utama ", 2_000_000, 15_000))

        val updated = dao.findById(7)!!
        assertEquals(7L, updated.id)
        assertEquals("BCA Utama", updated.name)
        assertEquals(2_000_000L, updated.initialBalance)
        assertEquals(15_000L, updated.monthlyFee)
    }

    @Test
    fun `archiving normal wallet keeps another default`() = runBlocking {
        val dao = FakeWalletDao(
            WalletEntity(id = 1, name = "Tunai", isDefault = true),
            WalletEntity(id = 2, name = "BCA", sortOrder = 1),
        )
        val repository = WalletRepository(dao)

        assertEquals(ArchiveResult.ARCHIVED, repository.archive(2))
        assertTrue(dao.findById(2)!!.isArchived)
        assertTrue(dao.findById(1)!!.isDefault)
    }

    @Test
    fun `archiving default promotes first remaining active wallet`() = runBlocking {
        val dao = FakeWalletDao(
            WalletEntity(id = 1, name = "Tunai", isDefault = true),
            WalletEntity(id = 2, name = "BCA", sortOrder = 1),
        )
        val repository = WalletRepository(dao)

        assertEquals(ArchiveResult.ARCHIVED, repository.archive(1))

        assertTrue(dao.findById(1)!!.isArchived)
        assertFalse(dao.findById(1)!!.isDefault)
        assertTrue(dao.findById(2)!!.isDefault)
    }

    @Test
    fun `last active wallet archive is refused without mutation`() = runBlocking {
        val dao = FakeWalletDao(WalletEntity(id = 1, name = "Tunai", isDefault = true))
        val repository = WalletRepository(dao)

        assertEquals(ArchiveResult.REFUSED_LAST_WALLET, repository.archive(1))
        assertFalse(dao.findById(1)!!.isArchived)
        assertTrue(dao.findById(1)!!.isDefault)
    }

    @Test
    fun `archived wallet cannot become default and can be restored`() = runBlocking {
        val dao = FakeWalletDao(
            WalletEntity(id = 1, name = "Tunai", isDefault = true),
            WalletEntity(id = 2, name = "Lama", sortOrder = 1, isArchived = true),
        )
        val repository = WalletRepository(dao)

        assertFalse(repository.setDefault(2))
        assertTrue(repository.unarchive(2))
        assertFalse(dao.findById(2)!!.isArchived)
    }

    private class FakeWalletDao(vararg seed: WalletEntity) : WalletDao {
        private val rows = seed.toMutableList()
        private var nextId = (rows.maxOfOrNull { it.id } ?: 0L) + 1L
        private val activeFlow = MutableStateFlow(activeRows())
        private val allFlow = MutableStateFlow(allRows())
        var transactionWrites: Int = 0

        override suspend fun insert(wallet: WalletEntity): Long {
            val id = if (wallet.id == 0L) nextId++ else wallet.id
            rows += wallet.copy(id = id)
            emit()
            return id
        }

        override suspend fun insertAll(wallets: List<WalletEntity>) {
            wallets.forEach { insert(it) }
        }

        override suspend fun count(): Int = rows.size
        override suspend fun activeCount(): Int = rows.count { !it.isArchived }
        override fun observeActive(): Flow<List<WalletEntity>> = activeFlow
        override suspend fun getActive(): List<WalletEntity> = activeRows()
        override fun observeAll(): Flow<List<WalletEntity>> = allFlow
        override suspend fun findById(id: Long): WalletEntity? = rows.firstOrNull { it.id == id }
        override suspend fun findActiveByName(name: String): WalletEntity? = rows.firstOrNull {
            !it.isArchived && it.name.equals(name, ignoreCase = true)
        }
        override suspend fun findDefault(): WalletEntity? = rows.firstOrNull {
            !it.isArchived && it.isDefault
        }
        override fun observeDefault(): Flow<WalletEntity?> = MutableStateFlow(
            rows.firstOrNull { !it.isArchived && it.isDefault }
        )
        override suspend fun findFirstActive(): WalletEntity? = activeRows().firstOrNull()

        override suspend fun clearDefaultFlags() {
            replaceAll { it.copy(isDefault = false) }
        }

        override suspend fun markDefault(id: Long) {
            replaceAll { if (it.id == id) it.copy(isDefault = true) else it }
        }

        override suspend fun setArchived(id: Long, archived: Boolean) {
            replaceAll { if (it.id == id) it.copy(isArchived = archived) else it }
        }

        override suspend fun updateDetails(
            id: Long,
            name: String,
            initialBalance: Long,
            monthlyFee: Long,
        ) {
            replaceAll {
                if (it.id == id) it.copy(
                    name = name,
                    initialBalance = initialBalance,
                    monthlyFee = monthlyFee,
                ) else it
            }
        }

        override fun observeBalances(): Flow<List<WalletBalance>> = MutableStateFlow(emptyList())
        override fun observeBalance(walletId: Long): Flow<Long?> = MutableStateFlow(null)

        private fun replaceAll(transform: (WalletEntity) -> WalletEntity) {
            rows.indices.forEach { index -> rows[index] = transform(rows[index]) }
            emit()
        }

        private fun emit() {
            activeFlow.value = activeRows()
            allFlow.value = allRows()
        }

        private fun activeRows() = rows.filterNot { it.isArchived }.sortedWith(walletOrder)
        private fun allRows() = rows.sortedWith(
            compareBy<WalletEntity> { it.isArchived }.then(walletOrder)
        )

        private companion object {
            val walletOrder = compareBy<WalletEntity>({ it.sortOrder }, { it.name })
        }
    }
}
