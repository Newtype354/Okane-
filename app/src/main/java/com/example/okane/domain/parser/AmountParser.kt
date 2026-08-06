package com.example.okane.domain.parser

/**
 * Shared utility for reading rupiah amounts out of notification text, so the
 * per-app parsers never solve number formatting themselves.
 *
 * Handles both Indonesian formatting ("Rp50.000", "Rp 1.234.567,00") and
 * English-style formatting some banking apps use ("IDR 50,000.00").
 * IDR has no usable decimals in practice, so decimal parts are dropped.
 */
object AmountParser {

    // "Rp50.000" / "Rp 50.000" / "Rp. 50.000" / "IDR 50,000.00" / "rp10.000"
    // \b prevents matching the "rp" inside words like "terpakai".
    // Capture must START with a digit so "Rp." alone can't match.
    private val RUPIAH_REGEX = Regex("""(?i)\b(?:Rp\.?|IDR)\s*([0-9][0-9.,]*)""")

    /**
     * Finds the FIRST rupiah amount in [text], e.g. "Pembayaran Rp50.000
     * berhasil" -> 50000. Returns null when no amount is present.
     */
    fun findRupiah(text: String): Long? =
        RUPIAH_REGEX.find(text)?.let { parseNumber(it.groupValues[1]) }

    /**
     * Parses the numeric part ("50.000", "1,234,567.89") into whole rupiah.
     *
     * Separator rules:
     *  - Both '.' and ',' present: the LAST one is the decimal separator
     *    ("1.234.567,00" and "1,234,567.00" both -> 1234567).
     *  - One kind, appearing multiple times: thousands ("1.234.567").
     *  - One kind, once, followed by exactly 3 digits: thousands ("50.000" —
     *    a money value with exactly 3 decimal places doesn't happen).
     *  - One kind, once, followed by 1-2 digits: decimal, dropped ("500.00").
     */
    fun parseNumber(raw: String): Long? {
        // Trailing punctuation belongs to the sentence, not the number
        // ("...sebesar Rp50.000." captures "50.000.").
        val s = raw.trim().trim('.', ',')
        if (s.isEmpty() || s.any { it !in '0'..'9' && it != '.' && it != ',' }) return null

        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')

        val integerPart = when {
            lastDot >= 0 && lastComma >= 0 ->
                s.substring(0, maxOf(lastDot, lastComma))
            lastDot >= 0 -> dropDecimalIfPresent(s, '.')
            lastComma >= 0 -> dropDecimalIfPresent(s, ',')
            else -> s
        }

        // Whatever separators remain are thousands separators — remove them.
        return integerPart.filter { it.isDigit() }.toLongOrNull()
    }

    private fun dropDecimalIfPresent(s: String, sep: Char): String {
        val first = s.indexOf(sep)
        val last = s.lastIndexOf(sep)
        val digitsAfterLast = s.length - last - 1
        val isThousands = first != last || digitsAfterLast == 3
        return if (isThousands) s else s.substring(0, last)
    }
}
