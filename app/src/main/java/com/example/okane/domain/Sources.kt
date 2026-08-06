package com.example.okane.domain

/**
 * Canonical display names of the supported financial apps.
 * Single source of truth: settings toggles key on these, and each
 * SourceParser's `source` property must use these constants, so the
 * toggle in Settings always matches what parsers write to the database.
 */
object Sources {
    const val GOPAY = "GoPay"
    const val SHOPEEPAY = "ShopeePay"
    const val LIVIN = "Livin"
    const val JENIUS = "Jenius"
    const val WONDR = "Wondr"
    const val BALE = "Bale"

    val ALL = listOf(GOPAY, SHOPEEPAY, LIVIN, JENIUS, WONDR, BALE)
}
