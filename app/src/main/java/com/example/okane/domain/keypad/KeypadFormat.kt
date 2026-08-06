package com.example.okane.domain.keypad

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * The amount display, shared by both surfaces so the keypad reads identically
 * everywhere. No Android imports — Surface A (Glance) and Surface B (Compose)
 * both call this.
 *
 * Locale.ROOT with an explicit grouping separator, not a locale-aware currency
 * formatter: the output must be exactly "Rp 500.000" on every device and in
 * JVM tests, and locale data varies by platform.
 *
 * Note this differs from [com.example.okane.ui.format.Formatters.rupiah],
 * which renders "Rp1.234.567" without the space for the dashboard and list.
 * The brief specifies the spaced form for the keypad display specifically.
 */
object KeypadFormat {

    private val grouped = DecimalFormat(
        "#,###",
        DecimalFormatSymbols(Locale.ROOT).apply { groupingSeparator = '.' }
    )

    /** 500000 -> "Rp 500.000"; 0 (and empty entry) -> "Rp 0". */
    fun rupiah(amount: Long): String =
        if (amount == 0L) "Rp 0" else "Rp " + grouped.format(amount)

    /** Convenience for the surfaces, which hold state rather than a number. */
    fun display(state: KeypadState): String = rupiah(state.amount)
}
