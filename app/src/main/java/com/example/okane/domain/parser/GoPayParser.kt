package com.example.okane.domain.parser

import com.example.okane.data.local.Transaction
import com.example.okane.domain.Sources

/**
 * Parser for the standalone GoPay app.
 *
 * Only handles notification formats CONFIRMED with real samples from the
 * user's device. Anything else returns null (and keeps being collected by
 * discovery capture, so unrecognized formats are never lost).
 */
class GoPayParser : SourceParser {

    override val source = Sources.GOPAY

    // Confirmed from device 2026-07-07. Note: the standalone GoPay app,
    // NOT the Gojek super-app (com.gojek.app).
    override val packageNames = setOf("com.gojek.gopay")

    // Confirmed sample (2026-07-07):
    //   title:   "Top up berhasil"
    //   text:    "Top up Rp10.001 via Mandiri udah masuk."
    // Captures: (1) the rupiah amount, (2) the funding channel ("Mandiri").
    // Trailing period optional — punctuation tolerance only, no guessing.
    private val topUpRegex =
        Regex("""^Top up (Rp[\d.,]+) via (.+?) udah masuk\.?$""")

    override fun parse(title: String?, text: String?): ParsedTransaction? {
        if (text == null) return null
        val trimmed = text.trim()

        // We match on text alone: this pattern is specific enough that a
        // title check would only add brittleness if GoPay tweaks titles.
        topUpRegex.find(trimmed)?.let { match ->
            val amount = AmountParser.findRupiah(match.groupValues[1]) ?: return null
            return ParsedTransaction(
                // Top up = money INTO the GoPay balance.
                type = Transaction.TYPE_KREDIT,
                amount = amount,
                merchant = "Top up via ${match.groupValues[2]}"
            )
        }

        // Payments, transfers, cashback, promos: no confirmed samples yet.
        return null
    }
}
