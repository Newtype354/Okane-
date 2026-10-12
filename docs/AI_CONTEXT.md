# Okane — konteks utama untuk AI chat

> Dokumen ini dibuat untuk menjembatani dua AI: **AI chat** dipakai untuk
> mendiskusikan produk dan menyusun spesifikasi, sedangkan **Codex CLI** membaca
> repository secara langsung dan mengimplementasikan hasil diskusi. Lampirkan
> dokumen ini pada awal percakapan AI chat. Di akhir diskusi, minta AI chat
> menghasilkan satu prompt implementasi yang siap ditempel ke Codex CLI.
> Repository yang dibaca Codex tetap menjadi kebenaran terakhir.
>
> Untuk diskusi pencatatan otomatis dari notifikasi, lampirkan juga
> `docs/riset-pencatatan-otomatis.md` (riset, status tiap aplikasi, desain
> Fase A, cara menambah template dari sampel, dan peta jalan).

## 1. Ringkasan singkat

**Okane** adalah aplikasi Android pencatat keuangan pribadi untuk pengguna bank
dan e-wallet Indonesia. Okane mempunyai dua jalur pencatatan yang menulis ke
database lokal yang sama:

1. pencatatan manual melalui keypad layar penuh; dan
2. pencatatan otomatis dari notifikasi aplikasi keuangan melalui Android
   `NotificationListenerService`.

Okane tidak terhubung ke bank, tidak membaca saldo rekening, dan tidak memiliki
backend. Karena data yang tercatat mungkin tidak lengkap, angka utama dashboard
bernama **“Tercatat bulan ini”**, bukan “saldo”, “tabungan”, atau
“Tabunganmu”. Istilah saldo hanya digunakan untuk saldo per dompet yang dihitung
dari saldo awal dan transaksi yang tersimpan.

Target produk saat ini adalah pencatatan lokal yang cepat, jujur, dan aman,
bukan aplikasi finansial lengkap dengan budget, investasi, atau sinkronisasi
bank.

## 2. Teknologi dan konfigurasi

- Kotlin 2.2.10
- Android Gradle Plugin 9.1.1
- Jetpack Compose + Material 3
- Plus Jakarta Sans 400/500/600/700 sebagai local bundled font family
- MVVM
- Kotlin Coroutines dan `Flow`/`StateFlow`
- Room 2.8.4, database schema versi 5
- Hilt 2.59.2 untuk dependency injection
- KSP untuk Room dan Hilt code generation
- SharedPreferences wrapper untuk onboarding dan toggle sumber notifikasi
- Glance 1.1.1 hanya untuk spike widget yang masih ditunda
- Compose motion: `AnimatedContent`, `animateItem`, `graphicsLayer` (lihat bagian 6)
- `minSdk 26`, `targetSdk 33`, `compileSdk 36.1`
- Java 11
- Satu module aplikasi: `app`
- Package/application id: `com.example.okane`

`androidx.core:core-ktx` sengaja dipatok ke 1.18.0. Versi 1.19.0 memerlukan
`compileSdk 37`, sedangkan proyek ini memakai 36.1.

## 3. Prinsip produk yang tidak boleh dilanggar

Saat memberi saran atau membuat rancangan, pertahankan invariant berikut kecuali
pengguna secara eksplisit memutuskan mengubah arah produk.

### 3.1 Keypad adalah jalur utama

Pencatatan notifikasi tidak pernah lengkap karena aplikasi dapat tidak
mengirimkan notifikasi atau pengguna dapat mematikannya. Oleh karena itu input
manual bukan fallback. Keypad dibuat seperti kalkulator agar transaksi dapat
disimpan dalam sedikit tap, tetapi tidak melakukan operasi aritmetika.

- Tidak ada tombol `+` atau `-`.
- Tombol komit adalah `Keluar`, `Masuk`, dan `Pindahkan`.
- Tombol `000` menambahkan tiga nol, bukan mengalikan seribu.
- Aturan digit berada di pure Kotlin `domain/keypad/KeypadState.kt`.

### 3.2 Nilai uang selalu rupiah utuh dan positif

- `amount` memakai `Long`, dalam rupiah utuh, tanpa floating point.
- Arah disimpan dalam enum `TransactionType`: `IN`, `OUT`, `TRANSFER`.
- Transfer adalah satu row dengan dompet asal dan tujuan, bukan pasangan
  pengeluaran + pemasukan.
- Transfer tidak masuk ke total pemasukan, pengeluaran, atau headline bulanan.

### 3.3 Saldo dompet dihitung, tidak disimpan

Tidak ada kolom balance. Rumus saldo adalah:

```text
initialBalance
  + IN ke dompet
  - OUT dari dompet
  + TRANSFER masuk melalui toWalletId
  - TRANSFER keluar melalui walletId
```

Query berada di `WalletDao.observeBalances()` dan mengembalikan `Flow`, sehingga
saldo ikut berubah ketika transaksi berubah.

### 3.4 Archive menggantikan delete

Wallet dan category diarsipkan, bukan dihapus, agar transaksi historis tidak
kehilangan nama referensinya. Item archived hilang dari picker tetapi masih
dapat di-resolve untuk histori. Transaksi boleh dihapus melalui mekanisme undo
setelah pencatatan manual.

`WalletRepository` menjaga dua invariant:

- wallet aktif terakhir tidak boleh diarsipkan;
- jika wallet default diarsipkan, wallet aktif lain dipromosikan menjadi default.

### 3.5 Transaksi tanpa kategori tetap valid

Category bersifat opsional saat input supaya pencatatan tetap cepat. Query
`uncategorised` dan aksi assign-category masih tersedia di data/ViewModel,
tetapi landing page Home yang minimal tidak merender inbox kategori (kartu
"Perlu konfirmasi" di Home adalah hal lain: saran dari notifikasi). Saat menambahkan
recovery UI di masa depan, tempatkan di surface lain atau ubah scope Home secara
eksplisit; jangan membuat category wajib hanya untuk menutupi tidak adanya UI.

### 3.6 Waktu transaksi dan waktu pencatatan berbeda

- `occurredAt`: kapan transaksi terjadi; dapat di-backdate.
- `createdAt`: kapan row ditulis; tidak di-backdate.

Keduanya tidak boleh digabung karena laporan harian harus memakai waktu
kejadian sebenarnya.

## 4. Arsitektur aktual

Ini adalah arsitektur yang benar-benar ada, bukan clean architecture ideal:

```text
Android entry points
  MainActivity / NotificationListenerService
                 │
Compose UI ← ViewModel (StateFlow)
                 │
             Repository
                 │
        Room DAO / preferences
                 │
          SQLite / local state
```

### Entry point

- `OkaneApp.kt`: hanya membawa `@HiltAndroidApp`.
- `MainActivity.kt`: Activity tunggal dan router aplikasi. Mengatur splash,
  onboarding, keypad full-screen, dan main tab screen tanpa Navigation Compose.
- `OkaneNotificationListenerService.kt`: entry point independen yang tetap
  berjalan saat UI tidak terbuka.

### UI

- `ui/OkaneMainScreen.kt`: tiga destination utama dan bottom navigation.
- `ui/dashboard/`: home screen dan full-bleed illustration hero.
- `ui/keypad/`: layar dan ViewModel input manual.
- `ui/onboarding/`: onboarding tiga slide dan state-nya.
- `ui/transactions/`: daftar seluruh transaksi.
- `ui/settings/`: permission dan toggle sumber notifikasi.
- `ui/components/`: komponen Compose bersama.
- `ui/format/Formatters.kt`: format rupiah dan waktu.
- `ui/theme/`: warna, typography, dan Material theme.

Screen umumnya stateless: menerima data dan callback. State dan operasi async
berada di ViewModel. Beberapa UI dan ViewModel menerima Room entity secara
langsung; belum ada UI-model mapping layer terpisah.

### Domain

- `domain/keypad/`: aturan keypad pure Kotlin dan mudah diuji.
- `domain/model/`: `TransactionType`, `TransactionSource`, `DashboardSummary`.
- `domain/Greeting.kt`: greeting berdasarkan jam.
- `domain/SummaryCalculator.kt`: ringkasan keuangan.
- `domain/capture/`: aturan pencatatan otomatis yang pure Kotlin — katalog
  sumber dan package (`SourceCatalog`), template berbasis data
  (`CaptureTemplate`/`KnownTemplates`), heuristik kata kunci, `CaptureEngine`,
  serta pencocokan duplikat dan transfer (`CaptureMatching`).
- `domain/parser/AmountParser.kt`: membaca nominal rupiah dari teks.
- `domain/Sources.kt`: nama sumber keuangan yang dikenal produk.

Layering tidak sepenuhnya murni: `SummaryCalculator` membaca
`TransactionEntity`, dan notification parser menghasilkan model yang tinggal di
`data.local`. Jangan mengasumsikan domain module dapat diekstrak tanpa refactor.

### Data

- `data/local/`: Room entities, DAO, database, migration.
- `data/repository/`: satu-satunya layer yang seharusnya mengakses DAO dari UI.
- `data/capture/`: `CaptureProcessor` (aturan tulis jalur notifikasi, satu
  mutex), `CaptureStore`/`RoomCaptureStore`, dan `CaptureRepository` untuk UI.
- `data/prefs/`: wrapper preferensi onboarding dan source toggle.
- `di/DatabaseModule.kt`: membangun Room serta memasang seluruh migration.
- `di/CaptureModule.kt`: menyediakan satu `CaptureProcessor` untuk service dan
  ViewModel.

## 5. Alur layar

`MainActivity` memilih surface dalam urutan berikut:

```text
fresh launch
  → animated splash
  → onboarding jika belum selesai
  → main screen
       ├─ Home
       ├─ Riwayat
       └─ Dompetku → Pengaturan (secondary screen)

Home FAB (+) → keypad full-screen → close/commit → kembali ke main screen
```

Keypad dan onboarding adalah full-screen surface tersendiri. Perubahan visual
home tidak boleh otomatis diterapkan pada kedua layar tersebut.

Greeting home selalu berasal dari `Greeting.now()` saat composition, sehingga
berubah sesuai waktu. Nilai yang berlaku saat ini adalah greeting Jepang
berdasarkan pagi/siang/malam; jangan hardcode `Ohayou` pada UI.

## 6. Home screen dan design system "calm"

Tampilan mengikuti gaya aplikasi mindfulness: bersih, minimalis, banyak ruang
kosong, sudut membulat, dan ilustrasi yang menenangkan (batu tumpuk dan daun).
Render referensi dari kode asli ada di `design/calm/` (PNG per layar) dan
`design/calm/motion/` (MP4 animasi). Mockup lama di `design/home-*.png` sudah
tidak berlaku.

Home (`ui/dashboard/`) bisa di-scroll vertikal dan berisi, dari atas:

- Greeting dinamis dari `Greeting.now()` (jangan hardcode) + subjudul, dengan
  tombol bulat Pengaturan di kanan atas.
- `TotalMoneyCard`: label **“Total uangmu”** dan total seluruh saldo wallet dari
  flow saldo yang sama dengan Dompetku (initialBalance + semua transaksi,
  wallet aktif dan archived, transfer netral), dengan ilustrasi batu tumpuk.
- Kartu peach **“N transaksi perlu konfirmasi”** (tag `homeCaptureInbox`),
  hanya muncul jika ada saran dari notifikasi. Kartu membuka lembar review
  satu per satu (`ui/capture/`).
- “Catat cepat”: empat tile pastel — Catat (buka keypad mode Masuk/Keluar,
  tag `homeAddTransaction`), Pindah (keypad mode transfer), Riwayat, Dompet.
- “Kebiasaanmu”: kartu catatan beruntun (streak) dengan cincin progres menuju
  milestone berikutnya. Logika di `domain/RecordingStreak.kt`: hari lokal dari
  `occurredAt`; streak belum putus sebelum hari berakhir.
- “Bulan ini”: pemasukan vs pengeluaran bulan berjalan (transfer dikecualikan,
  memakai `SummaryCalculator`).
- Kartu kutipan harian dari `domain/CalmQuotes.kt` (peribahasa atau kalimat
  Okane sendiri; jangan mengatribusikan kutipan ke orang nyata).

Data non-saldo Home berasal dari `TransactionViewModel.homeUiState`
(`HomePresenter`, pure dan teruji), yang ikut diperbarui saat tengah malam.

Bottom navigation: bar putih mengambang dengan tiga tujuan **Home**,
**Riwayat**, **Dompetku**. Indikator pil mengikuti posisi geser pager secara
langsung. Pengaturan bisa dibuka dari Home dan dari header Dompetku.

Design system (`ui/theme/`, `ui/components/Calm.kt`):

- Warna: latar `Paper` #FAFAF7, kartu putih dengan hairline, `Stone` untuk kartu
  hero, primary sage #4E7A5D, dan empat keluarga pastel (Mint, Mist, Lavender,
  Peach) untuk tile/chip. Warna semantik: IncomeGreen, ExpenseRed (clay),
  TransferBlue. Semua pasangan teks lolos kontras WCAG AA. Light only; dynamic
  color sengaja tidak aktif.
- Tipografi: Plus Jakarta Sans lokal untuk seluruh style Material.
- Ikon: garis tipis di `ui/icons/OkaneIcons.kt` (tanpa material-icons-extended).
- Ilustrasi: digambar dengan Canvas di `ui/illustration/CalmIllustrations.kt`,
  bisa dianimasikan lewat parameter `progress`. Tidak ada bitmap.
- Komponen: `CalmCard`, `PastelTile`, `CalmChip`, `CalmButton`/`CalmPrimaryButton`,
  `CalmFab`, `CalmBottomSheet`, `AnimatedAmountText`, `ProgressRing`. Pakai ini
  alih-alih menata komponen Material ad hoc.

Motion (`ui/theme/Motion.kt`, `OkaneMotion`): satu kosakata easing dan durasi
(200–450 ms, emphasized decelerate). Aturan agar tetap halus:

- Efek tekan (`Modifier.pressScale`) dan animasi masuk (`Modifier.calmEntrance`)
  berjalan lewat `graphicsLayer`, jadi tidak memicu recomposition per frame.
- Pergantian surface utama (`ui/navigation/OkaneRoute.kt`) memakai
  `AnimatedContent` dengan pola fade-through; keypad naik dari bawah,
  pengaturan masuk dari samping. `SaveableStateHolder` di MainActivity menjaga
  posisi scroll saat keypad dibuka.
- Daftar memakai `Modifier.animateItem()`; angka memakai `AnimatedAmountText`.
- Haptic ringan pada keypad (`VirtualKey`), chip (`SegmentTick`), dan commit
  (`Confirm`).

Splash Android 12+ dan ikon launcher memakai batu tumpuk sage yang sama
(`ic_splash_logo.xml`, `ic_launcher_*.xml`), dengan latar `@color/paper`.

## 7. Model database

Room database berada pada versi 5 dan mempunyai lima entity.

### `transactions`

Field penting:

| Field | Arti |
|---|---|
| `id` | primary key auto-generate |
| `amount: Long` | rupiah positif |
| `type` | `IN`, `OUT`, atau `TRANSFER` |
| `walletId` | wallet asal/utama, wajib |
| `toWalletId` | wallet tujuan; hanya transfer |
| `categoryId` | nullable; null untuk transfer atau belum dikategorikan |
| `occurredAt` | waktu transaksi |
| `createdAt` | waktu pencatatan |
| `source` | `MANUAL` atau `NOTIFICATION` |
| `sourceApp` | contoh `GoPay`; null untuk manual |
| `merchant` | hasil parser; nullable |
| `note` | catatan/raw text; nullable |

Unique index `(sourceApp, type, amount, occurredAt)` mencegah delivery
notifikasi ganda. Ia tidak membatasi input manual karena `sourceApp` manual
adalah null dan SQLite memperlakukan null sebagai distinct dalam unique index.

### `wallets`

Memiliki `name`, `initialBalance`, `monthlyFee`, `isDefault`, `sortOrder`, dan
`isArchived`. `monthlyFee` baru disimpan; belum ada scheduler yang memotongnya.

### `categories`

Memiliki `name`, `type`, `sortOrder`, dan `isArchived`. Category hanya boleh
bertipe `IN` atau `OUT`; transfer tidak memiliki category. Nama yang sama boleh
ada pada kedua tipe, misalnya “Lainnya”.

### `raw_notifications`

Menyimpan sampel notifikasi lokal untuk menulis template baru, hanya dari
sumber keuangan yang dikenal dan tanpa OTP. Data ini tidak dapat
direkonstruksi, sehingga destructive migration fallback sengaja dimatikan.

### `capture_suggestions` (v5)

Kotak "Perlu konfirmasi": notifikasi yang tampak seperti transaksi tetapi tidak
boleh dicatat sendiri. Kolomnya mencakup teks asli, `amount`, `direction`,
usulan `type`/`walletId`/`toWalletId`, `postedAt`, `status`
(`PENDING`/`ACCEPTED`/`DISMISSED`), `transactionId`, dan `duplicateOfId`. Saran
tidak dihapus setelah diselesaikan karena teksnya menjadi sampel template.
Rincian ada di `docs/riset-pencatatan-otomatis.md` bagian 5.5.

## 8. Dua jalur penulisan transaksi

### Manual

```text
KeypadScreen
 → KeypadViewModel
 → KeypadState memvalidasi input
 → TransactionRepository.addManual/addTransfer
 → TransactionDao.insert
 → Room
```

- Wallet wajib ada; repository memastikan fallback wallet `Tunai` tersedia.
- Double-tap commit dijaga oleh flag `committing`.
- Category yang arahnya tidak cocok dengan tombol `Masuk/Keluar` akan dibuang,
  bukan disimpan secara salah.
- Commit sukses membersihkan keypad dan membuat event undo.

### Notification listener

```text
Android notification
 → OkaneNotificationListenerService
 → SourceCatalog.identify: bukan sumber keuangan → dibuang tanpa dibaca
 → CaptureProcessor (mutex): toggle sumber → CaptureEngine.evaluate
      ├─ OTP/promo/gagal/tertunda/tanpa nominal → abaikan
      ├─ template terkonfirmasi + package terkonfirmasi → catat
      └─ selebihnya → capture_suggestions ("Perlu konfirmasi" di Home)
 → dedupe: notifikasi sama ±10 menit dibuang; uang yang mungkin sudah
   tercatat (termasuk catatan manual) ±30 menit → saran + peringatan
 → top up antar dompet sendiri → satu TRANSFER; sisi lain digabung,
   selisih ≤ Rp6.500 dicatat sebagai "Biaya admin"
 → sampel discovery (kecuali OTP) ke raw_notifications
```

Listener mengabaikan notifikasi Okane sendiri dan group summary. `bigText`
diprioritaskan atas text yang mungkin terpotong. Saat listener tersambung
ulang, notifikasi yang masih ada di panel diproses ulang (repeat dibuang).

Prinsip utamanya: **hanya template dari sampel asli, pada package yang
terkonfirmasi di HP pemilik, yang boleh mencatat sendiri.** Saat ini hanya top
up GoPay (`com.gojek.gopay`). ShopeePay, Livin', balé by BTN, Jenius, dan
wondr masih **lewat konfirmasi**, begitu juga SMS/WhatsApp/email dari bank.
Jangan menganggap aplikasi itu sudah didukung otomatis sebelum template-nya
ditulis dari sampel nyata dan diuji.

Dompet tujuan dicocokkan lewat nama atau alias sumber ("Mandiri" untuk Livin,
"BTN" untuk Bale), dengan fallback ke dompet default. Settings menampilkan
kesehatan listener (`CaptureHealth`), status per sumber, pengecualian baterai
dengan tips per merek HP, dan **Bagikan sampel** (teks tersensor lewat share
sheet).

## 9. Onboarding

Onboarding berjalan sekali dan mempunyai tiga slide. State disimpan di
`OnboardingViewModel`; tidak ada data yang ditulis sebelum `finish()`.

- Wallet suggestion: Tunai, BCA, Mandiri, BRI, BNI, BTN, Jenius, GoPay, OVO,
  DANA, ShopeePay.
- Wallet awalnya tidak terpilih. Jika semua dilewati, fallback `Tunai` dibuat.
- Category default awalnya terpilih agar skip tetap menghasilkan app yang
  dapat digunakan.
- Pengguna dapat membuat wallet/category custom.
- Saldo awal dan monthly fee wallet dapat diisi.
- Monthly fee belum diproses otomatis dan UI menjelaskan hal tersebut.

## 10. Ringkasan dan query penting

- `TransactionViewModel.transactions`: semua transaksi, terbaru dahulu.
- `summary`: dihitung ulang dari transaksi untuk bulan berjalan.
- `walletBalances`: hasil query gabungan wallet + transaksi.
- `uncategorised`: `categoryId IS NULL AND type != TRANSFER`.
- `recent`: maksimal 15 transaksi terbaru.
- Transfer dikecualikan dari income/expense dan headline.
- `DashboardSummary.netThisMonth = incomeThisMonth - expensesThisMonth`.

Catatan: `YearMonth.now()` dibaca ketika flow summary emit. Pergantian bulan
akan terlihat ketika aplikasi direstart atau ada emission transaksi berikutnya;
belum ada ticker khusus tepat tengah malam.

## 11. Batasan yang disengaja

Fitur berikut tidak tersedia pada wave saat ini dan jangan dipresentasikan
seolah sudah ada:

- multi-currency;
- sinkronisasi bank atau backend/cloud;
- budget, goal, chart, insight, atau export;
- arithmetic pada keypad;
- recurring monthly-fee scheduler;
- management UI lengkap untuk rename/archive/reorder wallet dan category;
- production home-screen widget;
- pencatatan otomatis selain top up GoPay (format lain masuk "Perlu
  konfirmasi" sampai template-nya ditulis dari sampel asli);
- template yang dipelajari dari konfirmasi pengguna, OCR bukti transfer, dan
  impor e-statement (Fase B–D di `docs/riset-pencatatan-otomatis.md`).

Repository sudah mempunyai sebagian operasi wallet/category management, tetapi
belum semuanya dapat dijangkau dari UI. Widget Glance yang terdaftar adalah
throwaway latency spike, bukan fitur produksi.

## 12. Testing dan migration safety

Perintah utama di Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
python tools/verify_sql.py
.\gradlew.bat connectedDebugAndroidTest  # memerlukan device/emulator
```

Test JVM mencakup keypad, formatter, greeting, summary calculator, rupiah
amount parser, onboarding state, dan seluruh pipeline capture: katalog sumber,
template GoPay (sampel asli), heuristik (teks sintetis), engine,
`CaptureProcessor` dengan store palsu (dedupe, transfer, biaya admin), ekspor
sampel, dan presenter inbox.

`OkaneDatabaseMigrationTest` memeriksa migration dengan Android test.
`tools/verify_sql.py` mengekstrak SQL migration dan balance query dari source,
menjalankan chain v2 → v3 → v4 → v5 pada SQLite, membandingkannya dengan schema
export Room di `app/schemas/`, lalu menjalankan setiap `@Query` DAO pada schema
terakhir.

Aturan database:

- `exportSchema = true` harus tetap aktif;
- file JSON `app/schemas/` harus dipertahankan;
- perubahan entity/DAO harus disertai migration jika mengubah schema;
- jangan menambahkan destructive fallback;
- jalankan `verify_sql.py` setelah mengubah entity, DAO query, atau migration.

Status verifikasi terakhir (Fase A pencatatan otomatis): unit test dan UI test
Compose dijalankan di harness desktop, dan `verify_sql.py` berhasil.
`assembleDebug`, `lintDebug`, dan `connectedDebugAndroidTest` belum dijalankan
untuk perubahan ini dan perlu dijalankan di mesin pengembang. Ini adalah
snapshot status pada saat dokumen dibuat, bukan jaminan untuk perubahan
berikutnya.

## 13. File yang paling relevan

| Area | File |
|---|---|
| Build | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| Routing | `MainActivity.kt`, `ui/OkaneMainScreen.kt` |
| Home | `ui/dashboard/DashboardScreen.kt`, `HomeHero.kt`, `HomeUiState.kt`, `domain/RecordingStreak.kt` |
| Design system | `ui/theme/`, `ui/components/Calm.kt`, `ui/icons/`, `ui/illustration/` |
| Routing animasi | `ui/navigation/OkaneRoute.kt` |
| Keypad | `ui/keypad/KeypadScreen.kt`, `KeypadViewModel.kt`, `domain/keypad/KeypadState.kt` |
| Onboarding | `ui/onboarding/OnboardingScreen.kt`, `OnboardingViewModel.kt` |
| Notification | `service/OkaneNotificationListenerService.kt`, `domain/capture/`, `data/capture/` |
| Perlu konfirmasi | `ui/capture/`, `data/local/CaptureSuggestionEntity.kt`, `CaptureDao.kt` |
| Database | `data/local/OkaneDatabase.kt`, entities, DAO, `Migrations.kt` |
| Data access | `data/repository/` |
| Theme | `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` |
| Tests | `app/src/test/`, `app/src/androidTest/`, `tools/verify_sql.py` |

Semua path pada tabel relatif terhadap
`app/src/main/java/com/example/okane/`, kecuali jika disebutkan lain.

## 14. Peran AI chat dan Codex CLI

Pembagian tanggung jawab yang diinginkan:

### AI chat

- Membantu mengeksplorasi masalah, kebutuhan pengguna, UX, edge case, dan
  trade-off.
- Menjaga keputusan diskusi tetap sesuai prinsip produk Okane.
- Mengubah keputusan yang sudah disepakati menjadi spesifikasi implementasi
  yang jelas dan tidak ambigu.
- Pada akhir diskusi, menghasilkan **prompt untuk Codex CLI**, bukan berpura-pura
  telah mengedit repository.
- Tidak perlu menebak detail baris kode yang tidak tersedia. Codex akan
  menginspeksi source aktual sebelum mengimplementasikan.

### Codex CLI

- Membaca repository dan instruksi lokal yang aktual.
- Memeriksa apakah asumsi dari AI chat cocok dengan implementasi saat ini.
- Mengimplementasikan perubahan dalam cakupan prompt.
- Menjaga perubahan pengguna yang tidak terkait.
- Menjalankan build, test, lint, atau pemeriksaan lain yang proporsional.
- Melaporkan hasil, file yang berubah, verifikasi, dan blocker jika ada.

Prompt hasil AI chat sebaiknya menjelaskan **outcome dan constraint**, bukan
memaksa patch baris-per-baris. Dengan begitu Codex dapat menyesuaikan solusi
dengan source terbaru yang benar-benar dibacanya.

## 15. Cara AI chat sebaiknya menggunakan konteks proyek

Ketika diminta merancang atau mengevaluasi perubahan:

1. Bedakan fitur yang benar-benar sudah diimplementasikan dari ide masa depan.
2. Pertahankan invariant finansial dan database pada bagian 3 dan 7.
3. Jangan mengklaim dapat melihat source terbaru jika hanya menerima dokumen
   ini; minta file terkait untuk perubahan yang membutuhkan detail baris kode.
4. Berikan solusi yang sesuai dengan Compose, Room, Hilt, Flow, dan satu-module
   architecture yang ada, kecuali tugasnya memang refactor arsitektur.
5. Pertimbangkan kedua producer transaksi setiap kali mengubah model atau
   persistence: keypad dan notification listener.
6. Pertimbangkan empty state, single-wallet state, archived references,
   uncategorised recovery, dan transfer pada perubahan UI/data.
7. Untuk perubahan schema, selalu sertakan entity + DAO + migration + exported
   schema + test plan.
8. Untuk perubahan Home, gunakan total saldo wallet all-time dari flow
   repository yang juga dipakai Dompetku, jangan hardcode greeting, dan pakai
   komponen serta `OkaneMotion` dari design system calm (bagian 6).
9. Jangan mengubah calculator atau onboarding hanya karena sedang mengubah home
   screen, kecuali cakupan tugas secara eksplisit mencakupnya.

## 16. Prompt pembuka untuk AI chat

Setelah melampirkan dokumen ini, pengguna dapat membuka percakapan dengan:

```text
Gunakan AI_CONTEXT.md sebagai konteks utama proyek Okane. Kamu adalah partner
diskusi produk dan penyusun spesifikasi; Codex CLI yang nanti membaca repository
dan mengimplementasikannya. Perlakukan keputusan produk dan invariant dalam
dokumen sebagai constraint. Jangan menganggap fitur yang tercantum sebagai
batasan sudah tersedia.

Selama diskusi, bantu aku memperjelas outcome, perilaku, state, edge case, dan
acceptance criteria. Jangan langsung membuat prompt final selama keputusan
penting masih belum disepakati. Jika detail source diperlukan untuk mengambil
keputusan produk, sebutkan file tambahan yang perlu aku lampirkan.

Ketika aku mengatakan "buat prompt untuk Codex", rangkum hasil diskusi menjadi
satu prompt implementasi mandiri yang mengikuti format pada AI_CONTEXT.md. Jangan
sertakan percakapan atau pilihan yang sudah ditolak.

Tugas yang ingin kita diskusikan: ...
```

## 17. Format prompt implementasi untuk Codex CLI

Saat pengguna mengatakan **“buat prompt untuk Codex”**, AI chat harus
menghasilkan satu blok prompt yang dapat langsung disalin. Gunakan struktur
berikut dan hilangkan bagian yang memang tidak relevan:

```markdown
# Task

[Jelaskan outcome akhir yang harus terlihat atau bekerja setelah implementasi.]

## Context

[Ringkasan singkat masalah dan keputusan final dari diskusi. Jangan mengulang
seluruh AI_CONTEXT.md. Sebutkan bahwa repository aktual adalah source of truth.]

## Requirements

- [Perilaku atau perubahan yang wajib ada]
- [State dan interaksi yang wajib ditangani]
- [Detail visual/data/business rule yang sudah disepakati]

## Must preserve

- [Invariant Okane yang relevan]
- [Flow atau screen yang tidak boleh berubah]
- [Kompatibilitas/migration/data yang harus dijaga]

## Scope

- Inspect dan ubah file yang diperlukan berdasarkan source aktual.
- Area yang kemungkinan relevan: `[path atau package]`.
- Jangan melakukan refactor di luar kebutuhan task.

## States and edge cases

- [Empty/loading/error/single-item/archived/transfer/etc. yang relevan]

## Acceptance criteria

- [Hasil yang dapat diperiksa secara objektif]
- [Tidak ada regresi yang relevan]

## Verification

- Jalankan build/test/lint yang proporsional dengan perubahan.
- [Perintah atau test khusus jika sudah disepakati]
- Laporkan apa yang diverifikasi dan keterbatasan verifikasi.

## Implementation guidance

- Baca `docs/AI_CONTEXT.md` dan source terkait sebelum mengubah kode.
- Anggap source repository aktual sebagai kebenaran terakhir jika ada detail
  teknis dalam prompt yang sudah tidak cocok.
- Jika ketidakcocokan dapat diselesaikan tanpa mengubah outcome, adaptasikan
  implementasi dan jelaskan. Jika mengubah outcome atau membutuhkan keputusan
  produk baru, berhenti dan tanyakan kepada pengguna.
```

### Aturan kualitas prompt Codex

Prompt final harus:

- mandiri: Codex tidak perlu melihat percakapan AI chat;
- hanya memuat keputusan final, bukan brainstorming atau opsi yang ditolak;
- menggunakan bahasa imperatif dan acceptance criteria yang dapat diuji;
- menyebutkan apa yang tidak boleh berubah jika ada risiko regresi;
- menjelaskan state dan edge case, bukan hanya happy path;
- memberi Codex kebebasan menginspeksi dan menyesuaikan struktur kode aktual;
- tidak mengarang nama class, function, dependency, atau nomor baris yang belum
  dikonfirmasi;
- tidak meminta Codex melewati build/test demi mengikuti solusi tertentu;
- menyebut migration dan data preservation jika model Room berubah;
- cukup lengkap untuk implementasi, tetapi tidak menyalin seluruh dokumen ini.

### Yang tidak perlu dimasukkan ke prompt final

- Sapaan atau narasi percakapan.
- Seluruh sejarah diskusi.
- Alternatif yang sudah ditolak.
- Penjelasan panjang mengenai prinsip Okane yang tidak berkaitan dengan task.
- Patch kode spekulatif dari AI chat, kecuali pengguna memang meminta algoritme
  atau kontrak teknis tertentu secara eksplisit.

## 18. Contoh permintaan penutup kepada AI chat

Setelah keputusan selesai, pengguna cukup menulis:

```text
Buat prompt untuk Codex berdasarkan keputusan final kita. Prompt harus mandiri,
siap ditempel ke Codex CLI, mempertahankan invariant Okane yang relevan, dan
memuat requirements, states/edge cases, acceptance criteria, serta verification.
Jangan masukkan opsi yang sudah kita tolak.
```

AI chat kemudian harus mengeluarkan prompt implementasi saja atau memberi label
yang jelas agar mudah disalin, tanpa mengklaim perubahan sudah diterapkan.

## 19. Batas dokumen

Dokumen ini cukup untuk diskusi produk, arsitektur, data model, flow, dan
penyusunan prompt implementasi. Ia tidak dapat menggantikan repository untuk
debugging error compiler, review baris-per-baris, memastikan signature API
terbaru, atau membuat patch yang presisi. Hal-hal tersebut adalah pekerjaan
Codex CLI setelah menerima prompt. Lampirkan source ke AI chat hanya jika detail
tersebut diperlukan untuk mengambil keputusan sebelum implementasi.
