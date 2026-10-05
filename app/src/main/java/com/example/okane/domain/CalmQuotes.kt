package com.example.okane.domain

import java.time.LocalDate

data class CalmQuote(val text: String, val source: String)

/**
 * A short, calm line for the bottom of Home, one per day.
 *
 * Indonesian proverbs are attributed to "Peribahasa"; Okane's own lines are
 * attributed to Okane. No quote is put in a real person's mouth.
 */
object CalmQuotes {

    private const val PROVERB = "Peribahasa"
    private const val OKANE = "Okane"

    val ALL: List<CalmQuote> = listOf(
        CalmQuote("Sedikit demi sedikit, lama-lama menjadi bukit.", PROVERB),
        CalmQuote("Hemat pangkal kaya.", PROVERB),
        CalmQuote("Berakit-rakit ke hulu, berenang-renang ke tepian.", PROVERB),
        CalmQuote("Tak perlu sempurna. Cukup dicatat, lalu dilepaskan.", OKANE),
        CalmQuote("Uang yang tercatat adalah uang yang kamu pahami.", OKANE),
        CalmQuote("Langkah kecil yang rutin lebih kuat dari niat besar.", OKANE),
        CalmQuote("Tarik napas. Satu transaksi, satu ketukan.", OKANE),
    )

    /** Stable for a whole local day, and different from one day to the next. */
    fun forDay(date: LocalDate): CalmQuote =
        ALL[Math.floorMod(date.toEpochDay(), ALL.size.toLong()).toInt()]
}
