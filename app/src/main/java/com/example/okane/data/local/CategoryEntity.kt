package com.example.okane.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.okane.domain.model.TransactionType

/**
 * A spending or income category.
 *
 * [type] is always [TransactionType.IN] or [TransactionType.OUT], never
 * TRANSFER — a category belongs to exactly one side, and transfers carry no
 * category at all. Tying a category to one direction is what lets the chip row
 * show only relevant options and what makes per-category reporting meaningful;
 * a category usable on both sides would net against itself.
 *
 * Archived, never deleted, for the same reason as wallets: deleting orphans
 * the historical rows that reference it.
 *
 * No foreign key from `transactions.categoryId` on purpose — a dangling id
 * degrades to "uncategorised", which the home screen inbox already surfaces,
 * whereas a cascade would silently rewrite history.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val type: TransactionType,

    val sortOrder: Int = 0,

    val isArchived: Boolean = false,
)

/**
 * Pre-seeded defaults, shown already ticked during onboarding so the user's
 * task is deletion rather than invention.
 */
object DefaultCategories {
    val EXPENSE: List<String> = listOf(
        "Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Lainnya",
    )

    val INCOME: List<String> = listOf(
        "Gaji", "Beasiswa", "THR", "Bonus", "Lainnya",
    )

    /** Both sides, in display order, ready to insert. */
    val ALL: List<CategoryEntity> =
        EXPENSE.mapIndexed { index, name ->
            CategoryEntity(name = name, type = TransactionType.OUT, sortOrder = index * 10)
        } + INCOME.mapIndexed { index, name ->
            CategoryEntity(name = name, type = TransactionType.IN, sortOrder = index * 10)
        }
}
