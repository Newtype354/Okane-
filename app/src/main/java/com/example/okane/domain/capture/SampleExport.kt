package com.example.okane.domain.capture

/** One captured notification, as shared for writing a new template. */
data class CapturedSample(
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
)

/**
 * "Bagikan sampel": turns captured notifications into plain text the owner
 * can paste into a chat to get a template written.
 *
 * Masking is on by default and errs towards hiding: account, card, phone and
 * reference numbers (any run of six or more digits that is not a rupiah
 * amount) and the name after "a.n." / "atas nama". Amounts stay, because a
 * template is written around them, and so do merchant names, because those
 * are what tell formats apart; the owner reviews the text before sending.
 */
object SampleExport {

    fun format(samples: List<CapturedSample>, mask: Boolean = true): String = buildString {
        appendLine("Sampel notifikasi Okane (${samples.size})")
        samples.forEachIndexed { index, sample ->
            appendLine()
            appendLine("#${index + 1} ${sample.packageName} @ ${sample.postedAt}")
            appendLine("title: ${sample.title?.let { if (mask) mask(it) else it }}")
            appendLine("text: ${sample.text?.let { if (mask) mask(it) else it }}")
        }
    }.trimEnd()

    fun mask(text: String): String {
        val withoutNames = HOLDER.replace(text) { it.groupValues[1] + "***" }
        return LONG_NUMBER.replace(withoutNames) { match ->
            val before = withoutNames.substring(maxOf(0, match.range.first - 4), match.range.first)
            if (AMOUNT_PREFIX.containsMatchIn(before)) match.value else "*".repeat(match.value.length)
        }
    }

    private val HOLDER = Regex("""(?i)(\ba\.?\s?n\.?\s+|\batas nama\s+)[\p{L} .']{2,40}?(?=\s*(?:[,;:()\-]|\s(?:rp|idr|sebesar|senilai|berhasil|telah|pada|di|ke|dari)\b|$))""")
    private val LONG_NUMBER = Regex("""(?<![\d.,])\d{6,}(?![\d])""")
    private val AMOUNT_PREFIX = Regex("""(?i)(rp\.?|idr)\s*$""")
}
