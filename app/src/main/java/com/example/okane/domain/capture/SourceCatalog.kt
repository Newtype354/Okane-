package com.example.okane.domain.capture

import com.example.okane.domain.Sources

/** Where a notification came from: the finance app itself, or a message relaying it. */
enum class CaptureChannel { APP, SMS, WHATSAPP, EMAIL }

enum class SourceKind { E_WALLET, BANK }

/**
 * How sure we are that a package name belongs to a source.
 *
 * The rule from the parser layer still holds: nothing is recorded
 * automatically from a package that has not been seen on a real device.
 * Unverified packages and messaging channels can only ever produce a
 * suggestion the user confirms.
 */
enum class PackageTrust {
    /** Seen posting real transaction notifications on the owner's phone. */
    CONFIRMED_ON_DEVICE,

    /** Published by the provider or a store listing, not yet seen on device. */
    PUBLIC_LISTING,

    /** Matched only by a keyword in the package name, or relayed by SMS/WA/email. */
    UNVERIFIED,
}

data class FinanceSource(
    /** One of [Sources]; settings toggles and `sourceApp` use this exact string. */
    val name: String,
    val kind: SourceKind,
    val packages: Map<String, PackageTrust>,
    /**
     * Words that identify this source in text: a wallet name, an SMS sender,
     * or a counterpart mentioned in another app's notification ("via Mandiri").
     */
    val aliases: List<String>,
    /**
     * Lowercase package-name segments that mark an unknown package as
     * probably this source: "btn" matches `id.co.btn.mobile`. A hint of six
     * letters or more may also sit inside a segment ("mandiri" in
     * `bankmandiri`); a short one must be the whole segment, so "livin" does
     * not claim `com.living.app`.
     */
    val packageHints: List<String>,
)

/** A notification attributed to a source, with how much it can be trusted. */
data class IdentifiedSource(
    val source: FinanceSource,
    val channel: CaptureChannel,
    val trust: PackageTrust,
)

/**
 * The finance apps Okane listens to, and the messaging apps that can relay
 * their alerts. Package names marked [PackageTrust.UNVERIFIED] or absent are
 * still being confirmed with discovery samples — see
 * docs/riset-pencatatan-otomatis.md for the checklist.
 */
object SourceCatalog {

    val GOPAY = FinanceSource(
        name = Sources.GOPAY,
        kind = SourceKind.E_WALLET,
        packages = mapOf(
            "com.gojek.gopay" to PackageTrust.CONFIRMED_ON_DEVICE,
            // The Gojek super-app also carries GoPay; its notifications are
            // mostly rides and food, so it must stay suggestion-only.
            "com.gojek.app" to PackageTrust.UNVERIFIED,
        ),
        aliases = listOf("GoPay", "Gojek", "Go-Pay"),
        packageHints = listOf("gopay", "gojek"),
    )

    val SHOPEEPAY = FinanceSource(
        name = Sources.SHOPEEPAY,
        kind = SourceKind.E_WALLET,
        packages = mapOf(
            // Named in ShopeePay's own partner integration guide.
            "com.shopeepay.id" to PackageTrust.PUBLIC_LISTING,
            // The Shopee marketplace app also posts ShopeePay alerts among
            // order updates and promos.
            "com.shopee.id" to PackageTrust.UNVERIFIED,
        ),
        aliases = listOf("ShopeePay", "Shopee"),
        packageHints = listOf("shopeepay", "shopee"),
    )

    val LIVIN = FinanceSource(
        name = Sources.LIVIN,
        kind = SourceKind.BANK,
        packages = mapOf("id.bmri.livin" to PackageTrust.UNVERIFIED),
        aliases = listOf("Livin", "Mandiri"),
        packageHints = listOf("bmri", "mandiri", "livin"),
    )

    val BALE = FinanceSource(
        name = Sources.BALE,
        kind = SourceKind.BANK,
        // Package not confirmed yet; recognised by its name until a sample
        // from the device pins it down.
        packages = emptyMap(),
        aliases = listOf("Bale", "BTN"),
        packageHints = listOf("btn", "bale"),
    )

    val JENIUS = FinanceSource(
        name = Sources.JENIUS,
        kind = SourceKind.BANK,
        // Store analytics list Jenius under this package (from its BTPN era).
        packages = mapOf("com.btpn.dc" to PackageTrust.PUBLIC_LISTING),
        aliases = listOf("Jenius", "SMBC", "BTPN"),
        packageHints = listOf("btpn", "jenius", "smbc"),
    )

    val WONDR = FinanceSource(
        name = Sources.WONDR,
        kind = SourceKind.BANK,
        packages = emptyMap(),
        aliases = listOf("Wondr", "BNI"),
        packageHints = listOf("wondr", "bni"),
    )

    val ALL: List<FinanceSource> = listOf(GOPAY, SHOPEEPAY, LIVIN, BALE, JENIUS, WONDR)

    /** Messaging apps whose notifications can relay a bank's SMS, WhatsApp or email. */
    private val MESSAGING: Map<String, CaptureChannel> = mapOf(
        "com.google.android.apps.messaging" to CaptureChannel.SMS,
        "com.samsung.android.messaging" to CaptureChannel.SMS,
        "com.android.mms" to CaptureChannel.SMS,
        "com.android.messaging" to CaptureChannel.SMS,
        "com.whatsapp" to CaptureChannel.WHATSAPP,
        "com.whatsapp.w4b" to CaptureChannel.WHATSAPP,
        "com.google.android.gm" to CaptureChannel.EMAIL,
        "com.microsoft.office.outlook" to CaptureChannel.EMAIL,
        "com.yahoo.mobile.client.android.mail" to CaptureChannel.EMAIL,
    )

    /**
     * Packages that look like a source by name but are not its money app:
     * "bale Community" is BTN's housing-estate app, not balé by BTN.
     */
    private val NOT_FINANCE: Set<String> = setOf("id.co.btn.smartresidence")

    fun byName(name: String): FinanceSource? = ALL.firstOrNull { it.name == name }

    /**
     * Attributes a notification to a source, or null when it has nothing to do
     * with money Okane tracks.
     *
     * Finance apps are matched by package. Messaging apps only count when the
     * sender (the notification title) is a known bank or wallet — otherwise a
     * friend's "transfer Rp50.000 ya" chat would become a transaction.
     */
    fun identify(packageName: String, title: String?): IdentifiedSource? {
        if (packageName in NOT_FINANCE) return null
        ALL.forEach { source ->
            source.packages[packageName]?.let { trust ->
                return IdentifiedSource(source, CaptureChannel.APP, trust)
            }
        }

        MESSAGING[packageName]?.let { channel ->
            val sender = title ?: return null
            val source = sourceMentionedIn(sender) ?: return null
            return IdentifiedSource(source, channel, PackageTrust.UNVERIFIED)
        }

        val segments = packageName.lowercase().split('.')
        ALL.forEach { source ->
            val hinted = source.packageHints.any { hint ->
                segments.any { it == hint || (hint.length >= 6 && hint in it) }
            }
            if (hinted) return IdentifiedSource(source, CaptureChannel.APP, PackageTrust.UNVERIFIED)
        }
        return null
    }

    /**
     * The first source named in [text] as a whole word, skipping [except].
     * "Top up Rp50.000 via Mandiri" names Livin's bank; that is how a top-up
     * is recognised as money moved between the user's own wallets.
     */
    fun sourceMentionedIn(text: String, except: FinanceSource? = null): FinanceSource? {
        var best: Pair<FinanceSource, Int>? = null
        ALL.forEach { source ->
            if (source == except) return@forEach
            source.aliases.forEach { alias ->
                val at = wordIndex(text, alias)
                if (at >= 0 && (best == null || at < best!!.second)) best = source to at
            }
        }
        return best?.first
    }

    /**
     * Whether a wallet the user named belongs to [source]: "Mandiri" and
     * "Livin" both belong to Livin, "BTN" to Bale. Matching aliases instead of
     * the exact source name is what lets a bank's app land in the wallet the
     * user called by the bank's name.
     */
    fun walletMatches(source: FinanceSource, walletName: String): Boolean =
        source.aliases.any { wordIndex(walletName, it) >= 0 }

    private fun wordIndex(text: String, word: String): Int {
        val pattern = Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(word) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.range?.first ?: -1
    }
}
