# Okane — riset & spesifikasi pencatatan otomatis

> Dokumen pendamping `docs/AI_CONTEXT.md`. Lampirkan **keduanya** di awal
> percakapan AI chat setiap kali membahas pencatatan otomatis dari notifikasi.
> AI_CONTEXT menjelaskan aplikasinya; dokumen ini menjelaskan riset, keputusan,
> apa yang sudah dibangun di Fase A, dan cara melanjutkannya. Repository tetap
> menjadi kebenaran terakhir. Jika isi dokumen ini berbeda dengan kode, ikuti
> kode lalu perbarui dokumen ini.

Status: **Fase A selesai** (Oktober 2026). Fase B–D belum dimulai.

---

## 1. Jawaban singkat

Mencatat transaksi dari notifikasi **realistis**, dengan target "sebagian besar
transaksi tercatat otomatis", bukan "semua". Notifikasi adalah satu-satunya
jalur yang gratis, real-time, berjalan sepenuhnya di HP tanpa server, dan lolos
kebijakan Google Play. Jalur ini juga tidak pernah lengkap: aplikasi bisa tidak
mengirim notifikasi, pengguna bisa mematikannya, sistem bisa menghentikan
listener, dan formatnya berubah tiap update aplikasi.

Karena itu Okane memakai **strategi berlapis** dengan satu aturan keselamatan:

> **Transaksi yang terlewat masih bisa dipulihkan. Transaksi palsu merusak
> saldo diam-diam.** Hanya format yang terkonfirmasi dari sampel asli, dari
> package yang terkonfirmasi di HP pemilik, yang boleh mencatat sendiri.
> Selebihnya hanya boleh *menyarankan*.

---

## 2. Aplikasi pemilik dan statusnya

Pemilik memakai lima aplikasi: **GoPay, ShopeePay, Livin' by Mandiri, balé by
BTN, Jenius (SMBC)**. wondr by BNI tetap ada di katalog karena sudah ada di
`Sources`, tetapi tidak menjadi prioritas.

| Aplikasi | `Sources` | Jenis | Package | Status package | Format otomatis | Catatan |
|---|---|---|---|---|---|---|
| GoPay | `GoPay` | e-wallet | `com.gojek.gopay` | **Terkonfirmasi di HP** (sampel 2026-07-07) | Top up | Super-app Gojek `com.gojek.app` juga membawa GoPay, tetapi belum terkonfirmasi |
| ShopeePay | `ShopeePay` | e-wallet | `com.shopeepay.id` | Listing publik (panduan partner ShopeePay, dokumentasi Airwallex) | – | Notifikasi ShopeePay juga bisa datang dari aplikasi Shopee `com.shopee.id` (belum terkonfirmasi) |
| Livin' by Mandiri | `Livin` | bank | `id.bmri.livin` | **Belum terkonfirmasi**; listing Play tidak ditemukan lewat pencarian | – | Notifikasi uang masuk/keluar bisa diatur dengan ambang Rp0–Rp5 juta di aplikasi |
| balé by BTN | `Bale` | bank | belum diketahui | **Belum terkonfirmasi** | – | Dikenali dari nama package (`btn`, `bale`). `id.co.btn.smartresidence` adalah aplikasi "bale Community" (perumahan), sengaja dikecualikan |
| Jenius | `Jenius` | bank | `com.btpn.dc` | Listing publik (Similarweb; penerbit PT Bank SMBC Indonesia) | – | Nama package berasal dari era BTPN |
| wondr by BNI | `Wondr` | bank | belum diketahui | – | – | Tidak dipakai pemilik |

Arti status (lihat `PackageTrust` di `domain/capture/SourceCatalog.kt`):

- **Terkonfirmasi di HP**: pernah terlihat mengirim notifikasi transaksi asli
  di HP pemilik. Hanya status ini yang boleh mencatat otomatis.
- **Listing publik**: disebut sumber resmi atau listing toko, belum terlihat
  di HP. Hanya menyarankan.
- **Belum terkonfirmasi**: hanya cocok dari kata di nama package, atau
  diteruskan lewat SMS/WhatsApp/email. Hanya menyarankan.

Cara memastikan package: buka halaman aplikasi di Play Store lewat browser, lalu
lihat `details?id=…` di URL. Bisa juga lihat hasil **Pengaturan → Contoh
notifikasi → Bagikan sampel**, yang mencantumkan package setiap notifikasi.

---

## 3. Peta semua sumber data

| Sumber | Bisa di Android + Play Store? | Real-time | Biaya | Catatan |
|---|---|---|---|---|
| **Notifikasi aplikasi** (bank, e-wallet) | ✅ Ya | ✅ | Gratis | Jalur utama Okane. Format berbeda-beda dan bisa berubah tiap update |
| **SMS lewat notifikasi** aplikasi SMS | ✅ Ya, tanpa izin `READ_SMS` | ✅ | Gratis | SMS bank juga muncul sebagai notifikasi Google Messages/Samsung Messages |
| SMS langsung (`READ_SMS`) | ⚠️ Sulit | ✅ | Gratis | Wajib lolos formulir deklarasi Play; ada developer yang ditolak |
| **WhatsApp dari bank** | ✅ Lewat notifikasi | ✅ | Gratis | Hanya sebagian bank yang memakainya |
| Email lewat Gmail API | ⚠️ Mahal | Hampir | Audit keamanan CASA tiap tahun | Scope `gmail.readonly` termasuk *restricted* dan butuh server |
| Email lewat notifikasi Gmail | ✅ Sebagian | ✅ | Gratis | Hanya pengirim, subjek, dan cuplikan; sering tanpa nominal |
| Share/screenshot bukti transfer + OCR di HP | ✅ Ya | Manual | Gratis | Rencana Fase C |
| Impor e-statement/mutasi (PDF/CSV) | ✅ Ya | Bulanan | Gratis | Paling lengkap; rencana Fase D untuk rekonsiliasi |
| Open banking (Brick dan sejenisnya) | ❌ Untuk sekarang | Hampir | Biaya per panggilan API, perlu badan usaha | Butuh server, kontrak, dan login internet banking pengguna |
| Accessibility Service (membaca layar) | ❌ | – | – | Play melarang penggunaan di luar keperluan aksesibilitas |
| iOS | ❌ | – | – | Apple tidak memberi akses notifikasi ke aplikasi lain |

**Tidak disarankan:** izin `READ_SMS`, Gmail API, Accessibility Service, dan
scraping internet banking.

---

## 4. Temuan riset

### Android

- **Android 15+ menyensor notifikasi berisi OTP** untuk notification listener
  pihak ketiga. Notifikasi transaksi biasa tidak terdampak. Jika bank
  menggabungkan OTP dan transaksi dalam satu notifikasi, isinya tidak terbaca.
  Okane sendiri tidak pernah menyimpan OTP.
- **APK di luar Play Store** terkena "Restricted settings" sejak Android 13
  (lebih ketat di Android 15). Pengguna harus membuka izin lewat Info Aplikasi
  sebelum bisa mengaktifkan akses notifikasi. Distribusi lewat Play lebih mulus.
- **Xiaomi, OPPO, vivo, realme, Samsung, dan Infinix** sering mematikan layanan
  latar belakang. Tanpa pengecualian baterai dan izin "mulai otomatis",
  notifikasi bisa terlewat tanpa tanda apa pun.

### Google Play

- Tidak ditemukan formulir deklarasi khusus untuk akses notifikasi. Yang
  berlaku adalah aturan umum data sensitif: pengungkapan jelas di dalam
  aplikasi, kebijakan privasi, dan formulir Data Safety.
- SMS berbeda. "SMS-based money management" tercantum sebagai penggunaan yang
  diizinkan, tetapi ada laporan aplikasi budgeting yang ditolak, dan Bluecoins
  memilih membuang fitur SMS.
- Okane membuka **daftar** optimasi baterai, bukan dialog langsung "izinkan?".
  Dialog langsung memerlukan izin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, yang
  dibatasi Play.
- Halaman Google Support tidak bisa diakses dari lingkungan riset, jadi detail
  kebijakan berasal dari hasil pencarian. **Cek ulang di Play Console sebelum
  rilis.**

### Bank dan e-wallet di Indonesia

- Tren bank bergeser dari SMS ke push notification. BCA menghentikan SMS
  Notifikasi untuk sebagian nasabah mulai 5 Januari 2026. Livin' punya
  notifikasi uang masuk/keluar dengan ambang yang bisa diatur. BRImo
  mengenakan biaya SMS sekitar Rp750 per pesan, sedangkan notifikasi di
  aplikasinya gratis.
- E-statement makin mudah didapat. Contohnya myBCA, yang menyediakan PDF
  mutasi hingga 5 tahun ke belakang.

### Pesaing dan regulasi

- Finku memakai model aggregator ("sambungkan akun" dengan login bank), bukan
  membaca notifikasi. Aggregator seperti Brick butuh kredensial bisnis, server
  callback, dan kadang OTP. Ini bertentangan dengan prinsip Okane yang lokal
  dan tanpa backend.
- **UU PDP** menggolongkan data keuangan pribadi sebagai data spesifik.
  Pemrosesan sepenuhnya di HP, tanpa server, adalah posisi paling aman.

### AI di HP

- Gemini Nano (ML Kit Prompt API) masih alpha dan hanya ada di HP flagship.
  ML Kit Entity Extraction tidak mendukung bahasa Indonesia.
- Parser utama tetap berbasis aturan. AI hanya bisa menjadi bonus di perangkat
  yang mendukung.

---

## 5. Fase A: yang sudah dibangun

### 5.1 Alur

```text
Notifikasi Android
 → OkaneNotificationListenerService
     ├─ SourceCatalog.identify(package, title)
     │    ├─ bukan sumber keuangan → dibuang (tidak dibaca, tidak disimpan)
     │    └─ sumber + saluran (APP/SMS/WHATSAPP/EMAIL) + tingkat kepercayaan
     ├─ CaptureHealth.seen(...)               (untuk kartu kesehatan)
     └─ CaptureProcessor.process(...)          (satu kunci/mutex untuk semua)
          ├─ toggle sumber mati → berhenti
          ├─ CaptureEngine.evaluate(...)
          │    ├─ OTP / promo / gagal / tertunda / tanpa nominal → abaikan
          │    ├─ template cocok + package terkonfirmasi → CATAT
          │    └─ selebihnya → SARANKAN
          ├─ notifikasi sama ±10 menit → abaikan (repeat)
          ├─ top up antar dompet sendiri → TRANSFER (+ pasangkan sisi lain)
          ├─ mungkin sudah tercatat → SARANKAN dengan peringatan duplikat
          └─ simpan ke transactions atau capture_suggestions
     → sampel discovery disimpan (kecuali OTP, repeat, sumber dimatikan)
```

Saat listener tersambung ulang, notifikasi yang masih ada di panel
(`getActiveNotifications()`) diproses lagi. Notifikasi yang sudah pernah
ditangani akan dibuang sebagai repeat.

### 5.2 File

| Area | File (relatif ke `app/src/main/java/com/example/okane/`) |
|---|---|
| Katalog sumber, package, alias | `domain/capture/SourceCatalog.kt` |
| Template berbasis data | `domain/capture/CaptureTemplate.kt` (`KnownTemplates`) |
| Aturan kata kunci | `domain/capture/TransactionHeuristics.kt` |
| Keputusan per notifikasi | `domain/capture/CaptureEngine.kt` |
| Pencocokan duplikat dan transfer | `domain/capture/CaptureMatching.kt` (`CaptureRules`) |
| Ekspor sampel tersensor, panduan baterai, status kesehatan | `domain/capture/SampleExport.kt`, `CaptureHealthState.kt` |
| Penyimpanan dan aturan tulis | `data/capture/CaptureProcessor.kt`, `CaptureStore.kt`, `CaptureRepository.kt` |
| Tabel saran | `data/local/CaptureSuggestionEntity.kt`, `CaptureDao.kt`, `MIGRATION_4_5` |
| Kesehatan listener | `data/prefs/CaptureHealth.kt` |
| Listener | `service/OkaneNotificationListenerService.kt` |
| UI "Perlu konfirmasi" | `ui/capture/` (kartu di Riwayat + lembar review) |
| UI kesehatan, baterai, bagikan sampel | `ui/settings/SettingsScreen.kt` |

`domain/parser/` kini hanya berisi `AmountParser`. `NotificationParser`,
`GoPayParser`, dan model `Transaction` lama sudah dihapus. Sampel GoPay beserta
tesnya dipindahkan ke `CaptureTemplateTest`.

### 5.3 Aturan keputusan

1. **Hanya sumber yang dikenal.** Aplikasi keuangan dikenali dari package-nya.
   SMS, WhatsApp, dan email hanya dihitung jika judul notifikasi (pengirim)
   adalah alias sumber yang dikenal, misalnya "BTN" atau "Jenius". Pesan teman
   "transfer Rp50.000 ya" tidak pernah dibaca.
2. **Catat otomatis hanya jika semua syarat terpenuhi:** saluran APP, package
   *terkonfirmasi di HP*, dan template dari sampel asli cocok. Saat ini syarat
   ini hanya terpenuhi oleh top up GoPay.
3. **Selebihnya menjadi saran.** Heuristik membaca nominal (melewati "saldo
   akhir", "sisa saldo", "limit", dan "biaya admin"), arah (kata masuk/keluar;
   top up berarti masuk untuk e-wallet dan keluar untuk bank), lawan transaksi
   (ke/dari/di/via), dan sumber lain yang disebut ("via Mandiri", "Top Up
   GoPay").
4. **Ditolak sejak awal:** OTP (tidak disimpan sama sekali, termasuk sebagai
   sampel), promo tanpa kata selesai, transaksi gagal atau dibatalkan,
   pengingat dan tagihan yang belum dibayar, teks tanpa nominal, dan teks
   tanpa kata transaksi.

### 5.4 Deduplikasi dan transfer

Konstanta ada di `CaptureRules`:

| Aturan | Jendela | Perilaku |
|---|---|---|
| Notifikasi yang sama diposting ulang | ±10 menit | Dibuang. Pembandingnya `note` transaksi dan judul/teks saran |
| Uang yang mungkin sudah tercatat (dompet, arah, dan nominal sama; **termasuk catatan manual**) | ±30 menit | Tidak dicatat otomatis. Menjadi saran dengan peringatan |
| Dua sisi satu transfer | ±15 menit, selisih ≤ Rp6.500 | Digabung menjadi satu `TRANSFER`. Selisihnya dicatat sebagai pengeluaran "Biaya admin" dari dompet asal |

Pemasangan transfer, dengan contoh **top up GoPay Rp100.000 dari Livin'
(Rp101.000 termasuk biaya)**:

- Jika pengguna punya dompet GoPay **dan** dompet Mandiri/Livin, notifikasi
  GoPay "Top up Rp100.000 via Mandiri udah masuk." dicatat sebagai
  `TRANSFER Mandiri → GoPay Rp100.000`, bukan pemasukan.
- Notifikasi Livin' "Top Up GoPay Rp101.000" menjadi saran yang menunjuk
  transfer tadi. Jika dikonfirmasi, transfer tidak digandakan. Okane hanya
  menambah biaya admin Rp1.000. Saldo akhirnya Mandiri −Rp101.000 dan GoPay
  +Rp100.000.
- Jika sisi lain sudah tercatat sebagai pemasukan atau pengeluaran biasa dari
  notifikasi dan belum dikategorikan, baris itu **diubah** menjadi transfer.
  Baris manual dan baris yang sudah dikategorikan **tidak pernah diubah**.
  Untuk kasus itu Okane hanya memberi peringatan lewat saran.
- Jika dompet salah satu sisi tidak ada, notifikasi dicatat sebagai
  pemasukan/pengeluaran biasa di dompet sumber, atau di dompet default.

Dompet dicocokkan lewat nama atau alias: dompet bernama "Mandiri", "Livin",
atau "Bank Mandiri" milik Livin; "BTN" milik Bale; "Jenius" milik Jenius.
Onboarding kini juga menawarkan dompet **BTN** dan **Jenius**.

### 5.5 Database v5

Tabel baru `capture_suggestions` (migrasi aditif `MIGRATION_4_5`; tidak ada
data lama yang diubah):

| Kolom | Arti |
|---|---|
| `sourceApp`, `packageName`, `channel` | Asal notifikasi |
| `title`, `text` | Teks asli. Inilah sampel untuk menulis template |
| `amount`, `direction` | Nominal dan arah menurut teks, dilihat dari dompet sumber |
| `type`, `walletId`, `toWalletId` | Usulan yang bisa diubah pengguna |
| `counterparty` | Lawan transaksi atau label |
| `postedAt`, `createdAt` | `postedAt` menjadi `occurredAt` saat dicatat |
| `status` | `PENDING` / `ACCEPTED` / `DISMISSED`. Baris tidak dihapus karena saran yang diabaikan juga sampel yang berguna |
| `transactionId`, `duplicateOfId`, `resolvedAt` | Hasil konfirmasi dan peringatan duplikat |

Skema ada di `app/schemas/…/5.json`. Diverifikasi oleh `tools/verify_sql.py`
(rantai v2→v5, semua query DAO) dan `OkaneDatabaseMigrationTest`.

### 5.6 UI

- **Riwayat → kartu "N transaksi perlu konfirmasi"** (pastel peach) di atas
  daftar. Kartu hanya muncul jika ada saran. Ikon Riwayat di bilah navigasi
  diberi titik peach selama ada saran. Home sengaja tidak menampilkannya,
  karena Home hanya berisi sapaan dan total uang.
- **Lembar review**: satu saran per layar ("1 dari 3"), menampilkan teks asli,
  peringatan duplikat, pilihan Keluar / Masuk / Pindah saldo, Dari/Ke dompet,
  serta tombol **Abaikan** dan **Catat**. Catat baru aktif setelah jenis dan
  dompet dipilih.
- **Pengaturan**: status "Tanpa batasan baterai" dengan tombol ke pengaturan
  baterai dan tips per merek HP, kartu **Kesehatan pencatatan otomatis**
  (notifikasi terakhir, terakhir dicatat otomatis, layanan tersambung), status
  per sumber ("Sebagian otomatis" / "Lewat konfirmasi" · terakhir terlihat),
  dan **Bagikan sampel**.
- Render ada di `design/calm/02-riwayat.png`, `14-review.png`, dan
  `10-pengaturan.png`.

### 5.7 Privasi

- Notifikasi dari aplikasi lain dibuang sebelum dibaca lebih jauh. Mode
  discovery lama yang menyimpan notifikasi "bernuansa keuangan" dari **semua**
  aplikasi sudah dihapus.
- OTP tidak pernah disimpan.
- Semua data tetap di HP. **Bagikan sampel** membuka share sheet Android dengan
  teks yang sudah disensor: deret 6+ digit selain nominal, serta nama setelah
  "a.n."/"atas nama". Pengguna memeriksa teks sebelum mengirimnya sendiri.
- Log debug hanya aktif pada build debuggable.

---

## 6. Mengumpulkan sampel dan menambah template

Template baru **hanya** boleh ditulis dari sampel asli. Inilah pekerjaan rutin
Fase B.

### 6.1 Persiapan di HP

1. Okane: Pengaturan → aktifkan akses notifikasi, lalu buka pengaturan baterai
   dan ikuti tips merek HP.
2. Di tiap aplikasi, aktifkan notifikasi transaksi:
   - **Livin'**: aktifkan notifikasi uang masuk dan keluar, lalu set ambang ke
     Rp0 agar semua transaksi memicu notifikasi.
   - **balé by BTN, Jenius, GoPay, ShopeePay**: aktifkan notifikasi transaksi
     atau promo di pengaturan masing-masing aplikasi. Promo juga berguna
     sebagai contoh negatif.
3. Jika bank juga mengirim SMS atau WhatsApp, biarkan aktif. Okane membacanya
   sebagai saran.

### 6.2 Daftar transaksi yang perlu disampel

Centang setelah sampelnya terkumpul. Satu sampel per baris sudah cukup untuk
memulai, dua sampel lebih baik untuk melihat bagian yang berubah.

| Aplikasi | Transaksi |
|---|---|
| GoPay | ☑ top up via bank · ☐ bayar QRIS · ☐ bayar di merchant/GoFood · ☐ kirim ke teman · ☐ terima dari teman · ☐ cashback/refund |
| ShopeePay | ☐ top up · ☐ bayar QRIS · ☐ bayar belanja Shopee · ☐ transfer/kirim · ☐ terima · ☐ refund |
| Livin' | ☐ transfer keluar (sesama Mandiri) · ☐ transfer keluar (BI-FAST/online) · ☐ uang masuk · ☐ top up GoPay/ShopeePay · ☐ QRIS · ☐ kartu debit |
| balé by BTN | ☐ transfer keluar · ☐ uang masuk · ☐ top up e-wallet · ☐ QRIS · ☐ SMS dari BTN (jika ada) |
| Jenius | ☐ transfer keluar · ☐ uang masuk · ☐ kartu debit/Visa · ☐ QRIS · ☐ top up e-wallet |

Pasangan yang paling berharga adalah **top up e-wallet dari bank**, karena
kedua sisinya terlihat (misalnya Livin' → GoPay). Sampel ini menguji pemasangan
transfer dan biaya admin.

### 6.3 Mengirim sampel ke alur AI chat

1. Okane → Pengaturan → Contoh notifikasi → **Bagikan sampel**.
2. Periksa teksnya, lalu tempel ke AI chat bersama dokumen ini.
3. Pakai prompt berikut:

```text
Ini sampel notifikasi asli dari HP-ku (sudah disensor). Gunakan
docs/riset-pencatatan-otomatis.md bagian 6.4. Untuk setiap format yang
muncul minimal sekali, usulkan CaptureTemplate (id, source, textPattern,
direction, ownMovement, label) dan tes yang mengutip sampelnya persis.
Tandai mana yang boleh dicatat otomatis (package harus terkonfirmasi)
dan mana yang tetap lewat konfirmasi. Jangan menebak format yang tidak
ada di sampel. Lalu buat prompt untuk Codex.

[tempel sampel di sini]
```

### 6.4 Aturan menulis template

- Template adalah data di `KnownTemplates` (`domain/capture/CaptureTemplate.kt`):

  ```kotlin
  CaptureTemplate(
      id = "gopay.topup.v1",
      source = Sources.GOPAY,
      textPattern = "Top up {amount} via {counterparty} udah masuk.",
      direction = MoneyDirection.IN,
      ownMovement = true,          // lawan transaksi bisa dompet sendiri
      label = "Top up via {counterparty}",
      sample = "com.gojek.gopay, 2026-07-07",
  )
  ```

- `{amount}` = nominal rupiah, `{counterparty}` = toko/orang/bank, `{any}` =
  bagian yang berubah tetapi tidak dipakai. Selebihnya harus cocok persis.
  Pencocokan mengabaikan huruf besar/kecil, spasi berlebih, dan titik di
  akhir.
- Template dicocokkan dengan **teks** notifikasi (bigText jika ada), bukan
  judul.
- `ownMovement = true` hanya untuk top up atau pindah dana yang lawannya bank
  atau e-wallet.
- Setiap template wajib punya tes di `CaptureTemplateTest` yang mengutip
  sampel aslinya **persis**, ditambah kasus yang harus ditolak (promo, gagal).
- Agar bisa mencatat otomatis, package sumbernya juga harus dinaikkan menjadi
  `CONFIRMED_ON_DEVICE` di `SourceCatalog`. Lakukan ini hanya jika package itu
  terlihat di sampel dari HP pemilik.
- Teks di `TransactionHeuristicsTest` adalah teks **sintetis**. Jangan pernah
  dijadikan template.

---

## 7. Batasan yang diketahui

- Kata kunci heuristik sengaja pendek dan berbahasa Indonesia. Format bank yang
  kalimatnya tidak lazim bisa diabaikan (`NO_SIGNAL`) atau arahnya kosong.
  Kasus itu masih aman karena pengguna yang memilih arahnya.
- Notifikasi yang memuat kata "gagal" atau "jatuh tempo" di kalimat tambahan
  ("jika gagal, hubungi…") akan ditolak. Ini disengaja: lebih baik terlewat
  daripada salah catat.
- WhatsApp grup yang namanya memuat alias bank, misalnya "Arisan BTN", bisa
  menghasilkan saran. Saran itu cukup diabaikan.
- Biaya admin hanya terdeteksi jika **kedua** sisi transfer terlihat. Jika
  hanya sisi bank yang terlihat, nominal transfer sudah termasuk biayanya.
- Notifikasi saat HP mati atau saat listener dihentikan sistem tidak bisa
  dipulihkan, kecuali masih ada di panel notifikasi. Fase D (rekonsiliasi
  e-statement) menutup celah ini.
- Android 15+ menyensor notifikasi yang mengandung OTP. Notifikasi gabungan
  OTP + transaksi tidak terbaca.
- Belum ada template selain top up GoPay. Untuk empat aplikasi lainnya, semua
  transaksi masih **lewat konfirmasi** sampai sampelnya terkumpul.

---

## 8. Peta jalan

1. **Fase A — selesai.** Mesin template + heuristik, kotak "Perlu konfirmasi",
   deduplikasi lintas sumber, pemasangan transfer + biaya admin, kesehatan
   listener + panduan baterai, perbaikan privasi discovery, dan Bagikan
   sampel.
2. **Fase B — berkelanjutan.**
   - Template dari sampel (bagian 6).
   - "Belajar dari contoh": saat pengguna mengonfirmasi saran, Okane mengusulkan
     template pribadi dengan mengganti nominal dan nama menjadi wildcard.
     Template ini disimpan di database, bukan di kode. Notifikasi berikutnya
     dengan format yang sama dicatat otomatis setelah pengguna menyetujuinya
     sekali.
   - Kategori otomatis dari merchant yang sama.
3. **Fase C — sekitar 1 minggu.** "Bagikan ke Okane" untuk teks atau
   screenshot bukti transfer, dengan OCR di HP (ML Kit Text Recognition,
   aksara Latin, gratis).
4. **Fase D — sekitar 1–2 minggu.** Impor e-statement PDF/CSV sebagai
   rekonsiliasi bulanan ("ada 3 transaksi yang belum tercatat"). Mulai dari
   bank milik pemilik (Mandiri, BTN, Jenius).
5. **Nanti saja.** Open banking lewat aggregator, hanya jika Okane menjadi
   bisnis dengan server dan siap mengurus kontrak serta aspek OJK.

---

## 9. Verifikasi Fase A

- Tes JVM baru: `SourceCatalogTest`, `CaptureTemplateTest`,
  `TransactionHeuristicsTest`, `CaptureEngineTest`, `CaptureProcessorTest`
  (store palsu di memori, termasuk skenario top up GoPay via Mandiri + biaya
  admin), `SampleExportTest`, dan `CaptureInboxPresenterTest`. Tes
  `AmountParserTest`, `FormattersTest`, dan `WalletRepositoryTest` diperluas.
- Tes UI: `CaptureInboxTest` (kartu konfirmasi, lembar review). Tes migrasi:
  `migratesTo5_addingTheInboxWithoutTouchingData`.
- `python tools/verify_sql.py`: rantai migrasi v2→v5 cocok dengan skema
  Room, dan semua `@Query` DAO berjalan di skema v5.
- Dijalankan di harness desktop (Compose Multiplatform). Build Android asli
  (`assembleDebug`, `lintDebug`, `connectedDebugAndroidTest`) **belum**
  dijalankan di lingkungan ini dan perlu dijalankan di mesin pengembang.

---

## 10. Sumber

- **Android:** [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-all) · [Android Authority: penyensoran OTP](https://www.androidauthority.com/android-15-sensitive-notifications-3416414) · [Android Authority: Android 16 lock screen](https://www.androidauthority.com/android-16-sensitive-notifications-lock-screen-3501564/) · [Restricted settings Android 13](https://bayton.org/android/android-13-restricted-permissions/) · [Android 15 Enhanced Confirmation Mode](https://androidauthority.com/android-15-enhanced-confirmation-mode-3436697) · [Panduan baterai per merek](https://help.biolovision.net/Keep_Background_Services_Alive_on_Android_(by_Brand))
- **Google Play:** [Kebijakan SMS/Call Log](https://support.google.com/googleplay/android-developer/answer/9047303) · [Pratinjau kebijakan izin sensitif](https://support.google.com/googleplay/android-developer/answer/16585319) · [Financial features declaration](https://support.google.com/googleplay/android-developer/answer/13849271?hl=en) · [Developer ditolak karena SMS](https://dev.to/zeta_byte/ive-been-working-on-a-personal-finance-app-called-finvantage-and-i-recently-hit-a-roadblock-4mjh) · [Bluecoins membuang SMS](https://www.bluecoinsapp.com/?p=9286)
- **Gmail API:** [Audit CASA (GMass)](https://www.gmass.co/blog/google-oauth-verification-security-assessment/) · [Biaya CASA (DeepStrike)](https://deepstrike.io/blog/google-casa-security-assessment-2025)
- **Package aplikasi:** [Panduan partner ShopeePay](https://product.shopeepay.co.id/integration/guide-for-partner/best-practices) · [Airwallex: aplikasi ShopeePay](https://www.airwallex.com/docs/payments/payment-methods/apac/shopeepay-beta/mobile-app-shopeepay) · [Similarweb: Jenius `com.btpn.dc`](https://similarweb.com/app/google/com.btpn.dc) · [balé by BTN](https://btn.co.id/en/Individual/Jasa-Layanan/e-Channels/bale-by-BTN)
- **Bank Indonesia:** [Notifikasi Livin' (IDX Channel)](https://www.idxchannel.com/banking/cara-mengaktifkan-notifikasi-livin-by-mandiri-untuk-uang-masuk-dan-keluar/all) · [BCA hentikan SMS notifikasi (Medcom)](https://www.medcom.id/ekonomi/bisnis/VNxAGlqb-mulai-5-januari-2026-bca-hentikan-sms-notifikasi) · [BCA mobile: atur notifikasi](https://www.bca.co.id/id/informasi/edukatips/2026/06/19/14/11/langkah-mudah-mengatur-notifikasi-di-bca-mobile) · [myBCA: notifikasi email](https://www.bca.co.id/id/informasi/edukatips/2026/04/15/09/53/mudahnya-mengatur-notifikasi-transaksi-email-di-mybca) · [BCA e-statement](https://www.bca.co.id/id/informasi/news-and-features/2025/12/18/09/09/Akses-Mutasi-Rekening-Kini-Lebih-Praktis-dan-Mudah) · [Mutasi 5 tahun di myBCA](https://momsmoney.kontan.co.id/news/tak-perlu-cetak-buku-rekening-ini-cara-cek-mutasi-5-tahunan-pakai-mybca) · [Biaya SMS BRImo (Mojok)](https://mojok.co/terminal/sms-brimo-meresahkan-bikin-nasabah-boncos/amp/)
- **Open banking dan pesaing:** [Brick: transaction list](https://docs.onebrick.io/docs/retrieve-transactions) · [Brick: getting user data](https://docs.onebrick.io/docs/getting-user-data) · [Keamanan Finku](https://teknologi.id/fintech/keamanan-finku-berstandar-militer-dan-bank-internasional) · [SNAP Bank Indonesia](https://www.bi.go.id/id/layanan/Standar/SNAP/default.aspx)
- **Regulasi:** [Siaran pers OJK POJK 3/2024](https://www.ojk.go.id/id/berita-dan-kegiatan/siaran-pers/Documents/Pages/OJK-Terbitkan-Aturan-Penyelenggaraan-Inovasi-Teknologi-Sektor-Keuangan-POJK-3-Tahun-2024/SP%20-%20OJK%20Terbitkan%20Aturan%20Penyelenggaraan%20Inovasi%20Teknologi%20Sektor%20Keuangan%20POJK%203%202024.pdf) · [UU PDP No. 27/2022](https://pasal.id/peraturan/uu/uu-no-27-tahun-2022) · [Jenis data pribadi (detik)](https://news.detik.com/berita/d-7517090/apa-saja-yang-termasuk-data-pribadi-simak-jenis-jenisnya-menurut-uu)
- **AI di HP:** [ML Kit Prompt API](https://developer.android.com/blog/posts/ml-kit-s-prompt-api-unlock-custom-on-device-gemini-nano-experiences) · [Bahasa ML Kit Entity Extraction (pihak ketiga)](https://pub.dev/documentation/learning_entity_extraction/latest/)
- **Contoh aplikasi lain:** [Parser notifikasi bank & batasan iOS (OlikSplit)](https://oliksplit.app/features/bank-notifications)
