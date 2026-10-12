package com.example.okane.domain.capture

import com.example.okane.domain.parser.AmountParser

/** Why a notification was judged not to be a finished transaction. */
enum class Rejection {
    /** One-time codes. Never stored, not even as a discovery sample. */
    OTP,

    /** Advertising: "cashback hingga Rp50.000", "yuk top up". */
    PROMO,

    /** Failed, cancelled or declined. */
    FAILED,

    /** Reminders and bills that have not been paid yet. */
    PENDING,

    /** No rupiah amount, or only a balance or limit. */
    NO_AMOUNT,

    /** An amount, but no word that says money moved. */
    NO_SIGNAL,
}

/** What the words of a notification say, before any wallet is involved. */
data class TextSignals(
    val rejection: Rejection?,
    val amount: Long?,
    /** "Biaya admin Rp2.500" when the text names its own fee. */
    val fee: Long?,
    /** Null when the words disagree or say nothing; the user picks. */
    val direction: MoneyDirection?,
    /** A top-up or transfer: money that may only have moved between own wallets. */
    val movement: Boolean,
    val counterparty: String?,
)

/**
 * General rules for notifications no template knows yet. Their output is
 * only ever a suggestion: words are too loose to record money on their own.
 *
 * The lists are deliberately short and Indonesian-first. Each word should be
 * one that a real notification of the five supported apps uses; widen them
 * from samples, not from imagination.
 */
object TransactionHeuristics {

    fun analyze(text: String, kind: SourceKind): TextSignals {
        val lower = text.lowercase()

        reject(lower)?.let { return empty(it) }

        val amounts = AmountParser.findAllRupiah(text)
        val fee = amounts.firstOrNull { isFee(text, it.range.first) }?.amount
        val amount = amounts.firstOrNull {
            it.amount > 0 && !isBalance(text, it.range.first) && !isFee(text, it.range.first)
        }?.amount ?: return empty(Rejection.NO_AMOUNT)

        val topUp = containsAny(lower, TOP_UP)
        val inScore = score(lower, IN_WORDS)
        val outScore = score(lower, OUT_WORDS)
        if (!topUp && inScore == 0 && outScore == 0 && !containsAny(lower, NEUTRAL)) {
            return empty(Rejection.NO_SIGNAL)
        }

        val direction = when {
            // A top-up fills an e-wallet and drains the bank that paid for it,
            // whatever else the sentence says.
            topUp -> if (kind == SourceKind.E_WALLET) MoneyDirection.IN else MoneyDirection.OUT
            inScore > outScore -> MoneyDirection.IN
            outScore > inScore -> MoneyDirection.OUT
            else -> null
        }
        val movement = topUp || containsAny(lower, TRANSFER_WORDS)
        return TextSignals(
            rejection = null,
            amount = amount,
            fee = fee,
            direction = direction,
            movement = movement,
            counterparty = counterparty(text, direction),
        )
    }

    /** Checked on its own before anything is stored, discovery samples included. */
    fun isOtp(text: String): Boolean = containsAny(text.lowercase(), OTP)

    private fun reject(lower: String): Rejection? = when {
        containsAny(lower, OTP) -> Rejection.OTP
        containsAny(lower, FAILED) -> Rejection.FAILED
        containsAny(lower, PENDING) -> Rejection.PENDING
        containsAny(lower, PROMO) && !containsAny(lower, DONE) -> Rejection.PROMO
        else -> null
    }

    private fun empty(rejection: Rejection) = TextSignals(
        rejection = rejection,
        amount = null,
        fee = null,
        direction = null,
        movement = false,
        counterparty = null,
    )

    /** "Saldo akhir: Rp…", "sisa saldo Rp…", "limit Rp…" describe, not move, money. */
    private fun isBalance(text: String, at: Int): Boolean {
        val before = text.substring(maxOf(0, at - 40), at)
        // "Top up saldo Rp50.000" and "isi saldo Rp…" are the movement itself.
        if (FILL_BEFORE.containsMatchIn(before)) return false
        return BALANCE_BEFORE.containsMatchIn(before)
    }

    private fun isFee(text: String, at: Int): Boolean =
        FEE_BEFORE.containsMatchIn(text.substring(maxOf(0, at - 24), at))

    private fun counterparty(text: String, direction: MoneyDirection?): String? {
        val order = when (direction) {
            MoneyDirection.IN -> listOf(FROM, TO, AT, VIA)
            MoneyDirection.OUT -> listOf(TO, AT, FROM, VIA)
            null -> listOf(TO, FROM, AT, VIA)
        }
        order.forEach { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.let(::cleanName)?.let { return it }
        }
        return null
    }

    private fun cleanName(raw: String): String? {
        val name = raw.trim().trim('.', ',', ':', '-').trim()
        if (name.length < 2 || name.length > 40) return null
        if (name.none { it.isLetter() }) return null
        if (AmountParser.findRupiah(name) != null) return null
        return name
    }

    private fun score(lower: String, words: List<String>): Int = words.count { contains(lower, it) }

    private fun containsAny(lower: String, words: List<String>): Boolean = words.any { contains(lower, it) }

    /** Whole-word match, so "masuk" does not hit "termasuk". */
    private fun contains(lower: String, word: String): Boolean {
        var from = 0
        while (true) {
            val at = lower.indexOf(word, from)
            if (at < 0) return false
            val before = lower.getOrNull(at - 1)
            val after = lower.getOrNull(at + word.length)
            if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) {
                return true
            }
            from = at + 1
        }
    }

    private val OTP = listOf(
        "otp", "kode verifikasi", "kode rahasia", "kode aktivasi", "kode keamanan",
        "jangan berikan", "jangan bagikan", "verification code", "do not share",
    )
    private val FAILED = listOf(
        "gagal", "dibatalkan", "ditolak", "tidak berhasil", "belum berhasil",
        "kedaluwarsa", "failed", "declined", "expired",
    )
    private val PENDING = listOf(
        "menunggu pembayaran", "segera bayar", "segera lakukan pembayaran", "jatuh tempo",
        "belum dibayar", "batas waktu", "akan didebet", "akan dipotong", "selesaikan pembayaran",
    )
    private val PROMO = listOf(
        "promo", "diskon", "voucher", "kupon", "gratis", "hemat", "s.d.", "s/d", "hingga",
        "up to", "yuk", "ayo", "buruan", "klaim", "dapatkan", "nikmati", "kesempatan",
        "spesial", "flash sale", "jangan lewatkan", "cek sekarang",
    )

    /** Words that say something already happened; they outweigh promo words. */
    private val DONE = listOf(
        "berhasil", "sukses", "telah", "sudah", "udah", "diterima", "terkirim",
        "dibayar", "menerima", "masuk ke", "uang masuk", "dana masuk",
    )
    private val TOP_UP = listOf("top up", "topup", "top-up", "isi saldo")
    private val TRANSFER_WORDS = listOf("transfer", "pindah dana", "kirim uang")
    private val IN_WORDS = listOf(
        "masuk", "uang masuk", "dana masuk", "transfer masuk", "diterima", "menerima",
        "terima uang", "dikreditkan", "credited", "refund", "pengembalian dana",
        "dikembalikan", "cashback", "bertambah", "incoming", "received",
    )
    private val OUT_WORDS = listOf(
        "uang keluar", "dana keluar", "pembayaran", "bayar", "membayar", "dibayar",
        "pembelian", "transfer ke", "kirim uang", "mengirim", "terkirim", "debit",
        "didebet", "didebit", "tarik tunai", "penarikan", "terpotong", "dipotong", "berkurang",
        "payment", "paid", "purchase", "qris",
    )
    private val NEUTRAL = listOf("transaksi", "transfer", "transaction")

    private val BALANCE_BEFORE = Regex(
        """(?i)(saldo|limit|minimal|min\.|maks(imal)?\.?|hingga|s\.?d\.?|s/d|up to)""" +
            """((\s+[\p{L}']+){0,2}\s*(:|=|sebesar|senilai|adalah|menjadi|jadi)|""" +
            """(\s+(akhir|anda|kamu|mu|tersedia|efektif|rekening|sekarang|saat ini))*)\s*$"""
    )
    private val FILL_BEFORE = Regex("""(?i)(top\s?-?up|isi|tambah)\s+saldo(\s+[\p{L}]+)?\s*$""")
    private val FEE_BEFORE = Regex("""(?i)(biaya( admin(istrasi)?)?|admin|fee)\s*(:|sebesar)?\s*$""")

    private const val STOP = """(?=\s+(?:sebesar|senilai|berhasil|telah|sudah|udah|pada|tgl|tanggal|via|dengan|di|rp|idr|sejumlah)\b|[.,;!(]|\s+-|$)"""
    private val TO = Regex("""(?i)\b(?:ke|kepada|to)\s+(.+?)$STOP""")
    private val FROM = Regex("""(?i)\b(?:dari|from)\s+(.+?)$STOP""")
    private val AT = Regex("""(?i)\b(?:di|at|merchant)\s+(.+?)$STOP""")
    private val VIA = Regex("""(?i)\bvia\s+(.+?)$STOP""")
}
