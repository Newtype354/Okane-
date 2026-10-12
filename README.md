# Okane

A personal finance tracker for Android, aimed at Indonesian bank and e-wallet
users. Two ways in: a keypad you can log a transaction on in a few taps, and a
`NotificationListenerService` that captures transactions from bank and e-wallet
notifications automatically.

Kotlin · Jetpack Compose · MVVM · Room · Hilt · minSdk 26 · targetSdk 33

---

## Building

There is no `java` on `PATH` in the usual development setup, so point Gradle at
Android Studio's bundled JDK first:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

**AndroidX version ceiling.** The project compiles against `compileSdk 36.1`.
AndroidX releases from around June 2026 (`androidx.core` 1.19.0 and later)
require `compileSdk 37` and fail `checkDebugAarMetadata`. `coreKtx` is pinned to
`1.18.0` for this reason — decline IDE suggestions to bump it.

## Verifying

```powershell
.\gradlew.bat testDebugUnitTest      # JVM unit tests
python tools/verify_sql.py           # migrations + balance SQL, no device needed
.\gradlew.bat connectedDebugAndroidTest   # needs a device or emulator
```

`tools/verify_sql.py` extracts the **actual** migration and balance SQL out of
the Kotlin sources, replays the full v2 → v3 → v4 → v5 migration chain against
SQLite, compares the result to Room's exported schema in `app/schemas/`, and
runs every DAO query against the final schema. Run it after changing any
entity, DAO query or migration. A mismatch between a
hand-written migration and Room's generated schema otherwise surfaces only as a
crash on a user's device, after their data is already at risk.

Room's schema export (`exportSchema = true`) must stay on, and the JSON files in
`app/schemas/` must be kept — they are the reference the migrations are checked
against.

## Visual design

The UI follows a calm, mindfulness-app style: warm paper background, white
cards with hairlines, a sage primary, soft pastel tiles, thin line icons and
Canvas-drawn stone and leaf illustrations. Motion is deliberate and shared
(`ui/theme/Motion.kt`): fade-through between surfaces, press feedback and
entrances on `graphicsLayer`, animated list items and amounts.

Renders of every screen, produced from the real Compose code, live in
`design/calm/`, with short motion clips in `design/calm/motion/`. The design
system is described in `docs/AI_CONTEXT.md`, section 6.

---

## Design rationale

### Entry is keypad-first

Notification capture is structurally incomplete: not every app emits a
transaction notification, and users can switch them off. Manual entry is
therefore the primary path, not a fallback, and it has to be fast enough that
logging costs almost nothing.

That rules out a form. The keypad is the whole screen — no navigation, no text
fields, no save dialog. "Calculator" describes the interaction shape, not
arithmetic: there is no chained addition and there are no `+` or `−` keys. The
commit buttons (`Keluar`, `Masuk`, `Pindahkan`) are the only operators in the
product.

The `000` key appends three zeros rather than multiplying by a thousand. A `rb`
multiply key produces unintended millions when pressed twice; appending is
simply what the user sees happen.

Input rules live in `domain/keypad/KeypadState`, a class with **no Android
imports**. The in-app screen holds that state in a ViewModel; a home screen
widget would have to persist it to DataStore, because its process can be killed
between two taps. Same rules, different storage — which only works if the rules
live somewhere neither surface owns.

### Amounts are positive; direction lives in a type enum

`amount` is always positive and `type` is `IN`, `OUT` or `TRANSFER`.

Reporting becomes `WHERE type = 'OUT'` instead of `WHERE amount < 0`.
Per-category totals need no `ABS()`. A whole family of sign-flip bugs on edit
simply cannot occur.

Most importantly, **a signed amount cannot express a transfer at all**: the same
row is negative for one wallet and positive for another. Moving money between
your own wallets is routine — topping up an e-wallet from a bank account — and
recorded as a paired `Keluar` + `Masuk` it would inflate both monthly totals for
money that never left your control. A transfer is one row, and it is excluded
from income, expense and the headline figure. Its effect appears only in the two
wallet balances it moves between.

Amounts are `Long` in whole rupiah. IDR has no decimals in practice, so no
floating point appears anywhere in the money path.

### Balances are computed, never stored

There is no balance column. A wallet's balance is derived on read:

```
initialBalance
  + IN       into this wallet
  - OUT      out of this wallet
  + TRANSFER arriving  (toWalletId = this wallet)
  - TRANSFER leaving   (walletId   = this wallet)
```

A stored balance drifts the moment any write path forgets to update it — an
undo, a migration, a future import — and tracing the discrepancy afterwards is
far more painful than recomputing. The query lives in `WalletDao.observeBalances`
and returns a `Flow`, so balances re-emit whenever a transaction changes and not
only when a wallet does.

### Archive replaces delete

Wallets and categories are archived, never deleted. Deleting one would orphan
every historical transaction that references it, and a slightly cluttered list
is much better than corrupted history. Archived items disappear from pickers but
stay resolvable, which is why an old transaction can still display the name of a
wallet you stopped using.

Two invariants are enforced in `WalletRepository`: the last active wallet cannot
be archived (`transactions.walletId` is `NOT NULL`, so zero wallets would mean
nothing could be recorded), and archiving the default promotes another wallet to
default.

Transactions themselves *are* deletable, but only through the ~5 second undo
affordance shown after a commit, which removes the row by id.

### The headline figure is not called a balance

The home screen headline reads **`Tercatat bulan ini`** — recorded this month —
not "balance".

Okane connects to no bank. That figure can only be the sum of what has been
recorded, and the entire premise of the project is that the record is
incomplete. Presenting it as a balance invites trust in a number the app cannot
stand behind. Onboarding says the same thing in the first slide, which is what
makes the conservative labelling read as honest rather than apologetic.

Balance language is reserved for **per-wallet** figures, which are complete
relative to each wallet's stated starting point, and for a future reconciliation
feature that could actually justify the word.

---

## Deliberate limitations

These are not oversights. Each was considered and excluded.

**No multi-currency.** Amounts are whole rupiah in a `Long`, with no minor
units anywhere. The keypad, the `000` key and the absence of decimals all depend
on this. IDR is hardcoded. Adding currencies would mean a currency column, a
rate source, a decision about historical rates, and a rewrite of every total.

**No monthly admin fee scheduling.** `monthlyFee` is stored on the wallet and
onboarding will collect it, but **nothing deducts it**. The onboarding screen
says so explicitly rather than letting users expect charges that never appear.
Recurring generation is its own subsystem — `WorkManager`, month-length edge
cases, catch-up after long device downtime, deduplication — and belongs in its
own wave. The column exists now so adding the scheduler later needs no
migration.

**No arithmetic on the keypad.** See above: the interaction shape is a
calculator, the behaviour is not.

**No budgets, goals, charts, or export.** Out of scope for this wave.

**Home screen widget deferred.** A Glance widget was designed and a latency
spike written for it, then deferred rather than cancelled. Widgets render as
`RemoteViews` in the launcher process, so every keypress is an inter-process
round trip; the spike exists to measure tap-to-pixel latency before committing
to a keypad that depends on it. The `KeypadState` seam described above is
preserved so adding the widget later needs no rework.

**No management UI for wallets and categories yet.** They can be created during
onboarding and inline from the calculator screen's pickers, so an empty list is
always recoverable. Renaming, archiving and reordering are implemented at the
repository level but not yet reachable from any screen.

---

## Notification capture

The listener reads notifications from the supported finance apps — GoPay,
ShopeePay, Livin' by Mandiri, Bale by BTN, Jenius and wondr — plus SMS,
WhatsApp and email that come *from* them. Every other app is dropped on arrival,
unread and unstored. The pipeline lives in `domain/capture/` (pure rules) and
`data/capture/CaptureProcessor` (storage):

- **Known templates record by themselves.** A format written from a real
  device sample, on a package confirmed on the device, is recorded directly.
  Today that is GoPay's top-up notification.
- **Everything else is a suggestion.** Unknown formats, unconfirmed packages
  and relayed SMS/WhatsApp/email go to *Perlu konfirmasi* on Home, pre-filled
  by keyword rules; nothing touches the balances until the user taps *Catat*.
- **No double counting.** The same notification re-posted is dropped; money
  that may already be recorded (including hand-typed rows) waits for
  confirmation; a top-up between the user's own wallets becomes one `TRANSFER`,
  and the other app's notification of it is merged rather than counted again,
  with any difference up to Rp6.500 recorded as the fee.
- **Health.** Settings shows when the listener last connected, the last
  finance notification and per-source last-seen times, plus battery-saver
  guidance per phone brand.

Samples (OTPs excluded) stay on the phone; *Bagikan sampel* shares them with
account numbers and recipient names masked, for writing new templates. The
research and the roadmap are in `docs/riset-pencatatan-otomatis.md`.

Because those discovery samples cannot be recovered once lost, destructive
migration fallback is **off**. A missing migration crashes loudly in development
instead of quietly wiping the database.
