package com.example.okane.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okane.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    /** Onboarding seeds idempotently, so it needs to know what already exists. */
    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findByNameAndType(name: String, type: TransactionType): CategoryEntity?

    /**
     * The side a category belongs to, for commit-time direction resolution.
     * Null when the id is unknown — a dangling reference resolves to
     * "uncategorised" rather than throwing.
     */
    @Query("SELECT type FROM categories WHERE id = :id")
    suspend fun findType(id: Long): TransactionType?

    @Query("SELECT * FROM categories WHERE isArchived = 0 AND type = :type ORDER BY sortOrder, name")
    fun observeActiveByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type, isArchived, sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    /**
     * Chip row: the most-used categories for one direction.
     *
     * LEFT JOIN so a category with no transactions still ranks (at zero)
     * rather than vanishing — otherwise a fresh install would show an empty
     * chip row, which is exactly when the user most needs the shortcut.
     */
    @Query(
        """
        SELECT c.* FROM categories c
        LEFT JOIN transactions t ON t.categoryId = c.id
        WHERE c.isArchived = 0 AND c.type = :type
        GROUP BY c.id
        ORDER BY COUNT(t.id) DESC, c.sortOrder ASC, c.name ASC
        LIMIT :limit
        """
    )
    fun observeMostUsedByType(type: TransactionType, limit: Int): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}
