package com.example.okane.domain.capture

/**
 * What the listener has been doing, for the health card in Settings. A
 * listener that quietly stopped (battery saver, an OEM task killer) looks
 * exactly like a quiet day, so the times are what tells them apart.
 */
data class CaptureHealthState(
    val listenerConnectedAt: Long? = null,
    /** Last notification from any supported source, recorded or not. */
    val lastNotificationAt: Long? = null,
    val lastSeenBySource: Map<String, Long> = emptyMap(),
    val lastRecordedAt: Long? = null,
)

/**
 * Brand-specific steps that keep a notification listener alive. Android's own
 * battery exemption is not enough on these skins; each also has its own
 * "autostart" or background switch.
 */
object BatteryGuide {

    fun tipFor(manufacturer: String): String? {
        val brand = manufacturer.lowercase()
        return when {
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ->
                "Xiaomi/Redmi/POCO: Setelan → Aplikasi → Okane → aktifkan Mulai otomatis, " +
                    "lalu Penghemat baterai → Tidak ada pembatasan."
            brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus") ->
                "OPPO/realme/OnePlus: Setelan → Baterai → Okane → izinkan aktivitas latar " +
                    "belakang dan Mulai otomatis."
            brand.contains("vivo") || brand.contains("iqoo") ->
                "vivo/iQOO: i Manager → Manajemen aplikasi → Mulai otomatis → aktifkan Okane, " +
                    "lalu izinkan konsumsi daya latar belakang tinggi."
            brand.contains("samsung") ->
                "Samsung: Setelan → Baterai → Batas penggunaan latar belakang → pastikan Okane " +
                    "tidak ada di daftar aplikasi tidur."
            brand.contains("huawei") || brand.contains("honor") ->
                "Huawei/Honor: Setelan → Baterai → Peluncuran aplikasi → Okane → kelola manual, " +
                    "aktifkan ketiga sakelar."
            brand.contains("infinix") || brand.contains("tecno") || brand.contains("itel") ->
                "Infinix/Tecno: Phone Master → Pengelolaan mulai otomatis → aktifkan Okane."
            else -> null
        }
    }
}
