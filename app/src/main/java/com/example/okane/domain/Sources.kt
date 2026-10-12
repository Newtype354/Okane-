package com.example.okane.domain

/**
 * Canonical display names of the supported financial apps.
 * Single source of truth: settings toggles key on these, and capture writes
 * them to `sourceApp`, so the toggle in Settings always matches the rows.
 * Package names, aliases and trust live in `domain/capture/SourceCatalog`.
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
