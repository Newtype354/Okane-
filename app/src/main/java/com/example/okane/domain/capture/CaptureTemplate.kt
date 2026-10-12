package com.example.okane.domain.capture

import com.example.okane.domain.Sources
import com.example.okane.domain.parser.AmountParser

/** Which way money moved, seen from the wallet of the app that posted it. */
enum class MoneyDirection { IN, OUT }

/**
 * A known notification format, written as data rather than code:
 *
 *     "Top up {amount} via {counterparty} udah masuk."
 *
 * `{amount}` is a rupiah amount, `{counterparty}` a store, person or bank, and
 * `{any}` skips words that change between notifications. Everything else must
 * match literally, ignoring case, extra spaces and a final full stop.
 *
 * Each template is written from a real sample seen on the owner's phone and
 * says so in [sample]. A template is never guessed: an unknown format becomes
 * a suggestion the user confirms, and its text becomes the sample for the
 * next template (see docs/riset-pencatatan-otomatis.md).
 */
data class CaptureTemplate(
    val id: String,
    /** One of [Sources]. */
    val source: String,
    val textPattern: String,
    val direction: MoneyDirection,
    /**
     * The counterparty is where the money came from or went to inside the
     * user's own money, as in a top-up. When the user has a wallet for it, the
     * notification is recorded as a transfer, not as income or spending.
     */
    val ownMovement: Boolean,
    /** Shown as the transaction's merchant; may use `{counterparty}`. */
    val label: String,
    /** Where and when the format was confirmed. */
    val sample: String,
) {
    private val regex: Regex by lazy { compile(textPattern) }
    private val slots: List<String> by lazy {
        PLACEHOLDER.findAll(textPattern).map { it.groupValues[1] }.filter { it != "any" }.toList()
    }

    /** The fields this template reads from [text], or null when it does not fit. */
    fun match(text: String): TemplateMatch? {
        val result = regex.find(normalise(text)) ?: return null
        val values = slots.zip(result.groupValues.drop(1)).toMap()
        val amount = values["amount"]?.let(AmountParser::findRupiah)?.takeIf { it > 0 }
            ?: return null
        val counterparty = values["counterparty"]?.trim()?.takeIf { it.isNotEmpty() }
        return TemplateMatch(
            template = this,
            amount = amount,
            counterparty = counterparty,
            label = label.replace("{counterparty}", counterparty.orEmpty()).trim(),
        )
    }

    private companion object {
        val PLACEHOLDER = Regex("""\{(amount|counterparty|any)\}""")
        const val AMOUNT = """((?:Rp\.?|IDR)\s*[0-9][0-9.,]*)"""

        fun compile(pattern: String): Regex {
            val body = StringBuilder()
            var last = 0
            val trimmed = pattern.trim().trimEnd('.', '!')
            PLACEHOLDER.findAll(trimmed).forEach { slot ->
                body.append(literal(trimmed.substring(last, slot.range.first)))
                body.append(
                    when (slot.groupValues[1]) {
                        "amount" -> AMOUNT
                        "counterparty" -> "(.+?)"
                        else -> ".*?"
                    }
                )
                last = slot.range.last + 1
            }
            body.append(literal(trimmed.substring(last)))
            return Regex("^$body[.!]?$", RegexOption.IGNORE_CASE)
        }

        fun literal(text: String): String =
            text.split(Regex("\\s+")).joinToString("\\s+") { if (it.isEmpty()) "" else Regex.escape(it) }
    }
}

data class TemplateMatch(
    val template: CaptureTemplate,
    val amount: Long,
    val counterparty: String?,
    val label: String,
)

/** Collapses the whitespace notifications are sloppy with. */
internal fun normalise(text: String): String = text.trim().replace(Regex("\\s+"), " ")

/**
 * Formats confirmed on the owner's phone. Add one only from a real sample;
 * the matching test should quote that sample exactly.
 */
object KnownTemplates {

    val GOPAY_TOP_UP = CaptureTemplate(
        id = "gopay.topup.v1",
        source = Sources.GOPAY,
        // Sample, title "Top up berhasil": "Top up Rp10.001 via Mandiri udah masuk."
        textPattern = "Top up {amount} via {counterparty} udah masuk.",
        direction = MoneyDirection.IN,
        ownMovement = true,
        label = "Top up via {counterparty}",
        sample = "com.gojek.gopay, 2026-07-07",
    )

    val ALL: List<CaptureTemplate> = listOf(GOPAY_TOP_UP)
}
