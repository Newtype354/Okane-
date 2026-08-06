package com.example.okane.data.repository

import com.example.okane.data.local.CategoryDao
import com.example.okane.data.local.CategoryEntity
import com.example.okane.data.local.DefaultCategories
import com.example.okane.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) {
    /** Every category including archived — for a management list. */
    val all: Flow<List<CategoryEntity>> = dao.observeAll()

    /** The full picker list for one direction. */
    fun activeByType(type: TransactionType): Flow<List<CategoryEntity>> =
        dao.observeActiveByType(type)

    /** The chip row: most-used first, for one direction. */
    fun chips(type: TransactionType, limit: Int = CHIP_LIMIT): Flow<List<CategoryEntity>> =
        dao.observeMostUsedByType(type, limit)

    /** Which side a category belongs to; null if the id is unknown. */
    suspend fun typeOf(categoryId: Long): TransactionType? = dao.findType(categoryId)

    /**
     * Idempotent by (name, type): re-running never duplicates. Onboarding
     * relies on this, and so does the upgrade path, where a user arriving from
     * an older version already has some of these rows.
     */
    suspend fun create(name: String, type: TransactionType): Long {
        val trimmed = name.trim()
        dao.findByNameAndType(trimmed, type)?.let { return it.id }
        return dao.insert(CategoryEntity(name = trimmed, type = type))
    }

    /**
     * Ensures the full default set exists, adding only what is missing.
     *
     * The skip-everything path calls this, as does the upgrade path — a user
     * migrating from v3 keeps their existing categories and gains the income
     * ones that version never had.
     */
    suspend fun ensureDefaults() {
        DefaultCategories.ALL.forEach { category ->
            if (dao.findByNameAndType(category.name, category.type) == null) {
                dao.insert(category)
            }
        }
    }

    suspend fun archive(id: Long) = dao.setArchived(id, true)

    suspend fun unarchive(id: Long) = dao.setArchived(id, false)

    companion object {
        const val CHIP_LIMIT = 5
    }
}
