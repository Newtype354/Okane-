package com.example.okane.ui.format

import com.example.okane.domain.model.TransactionType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display formatting for screens. Built by hand on DecimalFormat (instead of
 * NumberFormat.getCurrencyInstance) so the output is EXACTLY "Rp50.000" on
 * every device and in JVM unit tests — locale data can differ per platform.
 *
 * Only call from the UI (main) thread: DecimalFormat is not thread-safe.
 */
object Formatters {

    private val rupiahFormat = DecimalFormat(
        "#,###",
        DecimalFormatSymbols(Locale.ROOT).apply { groupingSeparator = '.' }
    )

    private val dateTimeFormat =
        DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID"))

    private val dateFormat =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))

    private val fullDateFormat =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))

    private val timeFormat =
        DateTimeFormatter.ofPattern("HH.mm", Locale.forLanguageTag("id-ID"))

    /** 1234567 -> "Rp1.234.567" */
    fun rupiah(amount: Long): String = "Rp" + rupiahFormat.format(amount)

    /** Balance/total formatting keeps the sign before the currency marker. */
    fun balanceRupiah(amount: Long): String = when {
        amount < 0 -> "-" + rupiah(-amount)
        else -> rupiah(amount)
    }

    /**
     * Adds a direction marker: IN -> "+Rp...", OUT -> "-Rp...".
     * TRANSFER gets no sign — it is neither a gain nor a loss overall, only a
     * move between two of the user's own wallets.
     */
    fun signedRupiah(amount: Long, type: TransactionType): String = when (type) {
        TransactionType.IN -> "+" + rupiah(amount)
        TransactionType.OUT -> "-" + rupiah(amount)
        TransactionType.TRANSFER -> rupiah(amount)
    }

    /** Epoch millis -> "7 Jul 2026" in the phone's timezone. */
    fun date(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormat)

    /** Epoch millis -> "7 Jul 2026 · 14:30" in the phone's timezone. */
    fun dateTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateTimeFormat)

    /** Epoch millis -> local 24-hour time, e.g. "14.30". */
    fun time(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .format(timeFormat)

    fun historyDateLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> date.format(fullDateFormat)
    }

    /**
     * How long ago, for status lines: "baru saja", "5 menit lalu",
     * "3 jam lalu", "kemarin 14.30", then a date.
     */
    fun relative(epochMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val minutes = (nowMillis - epochMillis) / 60_000L
        val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return when {
            minutes < 1 -> "baru saja"
            minutes < 60 -> "$minutes menit lalu"
            minutes < 12 * 60 && then.toLocalDate() == today -> "${minutes / 60} jam lalu"
            then.toLocalDate() == today -> "hari ini " + then.format(timeFormat)
            then.toLocalDate() == today.minusDays(1) -> "kemarin " + then.format(timeFormat)
            else -> then.format(dateFormat)
        }
    }

    fun netRupiah(amount: Long): String = when {
        amount > 0 -> "+" + rupiah(amount)
        amount < 0 -> "-" + rupiah(-amount)
        else -> rupiah(0)
    }
}
