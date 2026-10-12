package com.example.okane.domain.capture

/** One notification as the listener saw it. [text] is the richest text available. */
data class CaptureInput(
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
)

/** What a notification says happened, before it is matched to wallets. */
data class CaptureDraft(
    val source: FinanceSource,
    val channel: CaptureChannel,
    val amount: Long,
    /** Null when the words do not say; the user picks. */
    val direction: MoneyDirection?,
    val counterparty: String?,
    /** Another supported app or bank named in the text ("via Mandiri"). */
    val counterpartSource: FinanceSource?,
    /** A top-up or transfer, so [counterpartSource] may be the user's own wallet. */
    val movement: Boolean,
    /** A fee the text itself names, kept for display. */
    val fee: Long?,
    /** Shown as the merchant when recorded. */
    val label: String?,
    /** Set when a known template matched. */
    val templateId: String?,
)

enum class IgnoreReason {
    NOT_A_SOURCE,
    EMPTY,
    OTP,
    PROMO,
    FAILED,
    PENDING,
    NO_AMOUNT,
    NO_SIGNAL,
}

sealed interface CaptureDecision {
    data class Ignore(val reason: IgnoreReason) : CaptureDecision

    /** A confirmed template on a confirmed package: safe to record directly. */
    data class Record(val draft: CaptureDraft) : CaptureDecision

    /** Plausible, but the user decides: goes to "Perlu konfirmasi". */
    data class Suggest(val draft: CaptureDraft) : CaptureDecision
}

/**
 * Decides what one notification means. Pure: no clock, no storage, no Android.
 *
 * The one rule that matters: only a template confirmed from a real sample, on
 * a package confirmed on the owner's phone, records money by itself. Every
 * other path — unknown formats, unconfirmed packages, SMS, WhatsApp, email —
 * can at most suggest. A missed transaction is recoverable; a false one
 * silently corrupts balances.
 */
class CaptureEngine(
    private val templates: List<CaptureTemplate> = KnownTemplates.ALL,
) {

    fun evaluate(input: CaptureInput): CaptureDecision {
        val identified = SourceCatalog.identify(input.packageName, input.title)
            ?: return CaptureDecision.Ignore(IgnoreReason.NOT_A_SOURCE)
        return evaluate(input, identified)
    }

    fun evaluate(input: CaptureInput, identified: IdentifiedSource): CaptureDecision {
        val text = input.text?.let(::normalise).orEmpty()
        if (text.isEmpty()) return CaptureDecision.Ignore(IgnoreReason.EMPTY)

        // The title often carries the direction ("Transfer Masuk"), so the
        // words are read from both; templates stay strict and read the text.
        val words = listOfNotNull(input.title?.let(::normalise), text)
            .filter { it.isNotEmpty() }
            .joinToString(". ")
        if (TransactionHeuristics.isOtp(words)) return CaptureDecision.Ignore(IgnoreReason.OTP)

        val source = identified.source
        if (identified.channel == CaptureChannel.APP) {
            templates.asSequence()
                .filter { it.source == source.name }
                .firstNotNullOfOrNull { it.match(text) }
                ?.let { match -> return fromTemplate(match, identified, words) }
        }

        val signals = TransactionHeuristics.analyze(words, source.kind)
        signals.rejection?.let { return CaptureDecision.Ignore(it.toIgnoreReason()) }
        val amount = signals.amount ?: return CaptureDecision.Ignore(IgnoreReason.NO_AMOUNT)
        return CaptureDecision.Suggest(
            CaptureDraft(
                source = source,
                channel = identified.channel,
                amount = amount,
                direction = signals.direction,
                counterparty = signals.counterparty,
                counterpartSource = SourceCatalog.sourceMentionedIn(text, except = source),
                movement = signals.movement,
                fee = signals.fee,
                label = signals.counterparty,
                templateId = null,
            )
        )
    }

    private fun fromTemplate(
        match: TemplateMatch,
        identified: IdentifiedSource,
        words: String,
    ): CaptureDecision {
        val template = match.template
        val counterpart = match.counterparty
            ?.let { SourceCatalog.sourceMentionedIn(it, except = identified.source) }
        val draft = CaptureDraft(
            source = identified.source,
            channel = identified.channel,
            amount = match.amount,
            direction = template.direction,
            counterparty = match.counterparty,
            counterpartSource = if (template.ownMovement) counterpart else null,
            movement = template.ownMovement,
            fee = TransactionHeuristics.analyze(words, identified.source.kind).fee,
            label = match.label,
            templateId = template.id,
        )
        return if (identified.trust == PackageTrust.CONFIRMED_ON_DEVICE) {
            CaptureDecision.Record(draft)
        } else {
            CaptureDecision.Suggest(draft)
        }
    }

    private fun Rejection.toIgnoreReason(): IgnoreReason = when (this) {
        Rejection.OTP -> IgnoreReason.OTP
        Rejection.PROMO -> IgnoreReason.PROMO
        Rejection.FAILED -> IgnoreReason.FAILED
        Rejection.PENDING -> IgnoreReason.PENDING
        Rejection.NO_AMOUNT -> IgnoreReason.NO_AMOUNT
        Rejection.NO_SIGNAL -> IgnoreReason.NO_SIGNAL
    }
}
