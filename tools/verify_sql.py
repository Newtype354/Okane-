"""
Executes Okane's real migration and balance SQL against SQLite.

    python tools/verify_sql.py

No Android device is needed. The SQL is extracted from the Kotlin sources, so
this checks the actual shipped strings rather than a copy that could drift.
Re-run it after changing any entity, DAO query or migration — a mismatch
between a hand-written migration and Room's generated schema otherwise only
surfaces as a crash on a user's device, after their data is already at risk.

  1. Build a v2 database, run MIGRATION_2_3 then MIGRATION_3_4.
  2. Compare the result against Room's exported app/schemas/**/4.json, then
     run MIGRATION_4_5 and compare against 5.json.
  3. Check the data mapping (KREDIT->IN, app name -> sourceApp, wallet assignment).
  4. Run WalletDao's balance SQL over fixtures with transfers both ways.
  5. Prepare every @Query in every DAO against the final schema, and run the
     capture queries whose behaviour matters (NULL titles, guarded updates).

The device-side equivalent is app/src/androidTest/.../OkaneDatabaseMigrationTest,
which needs `gradlew connectedDebugAndroidTest`.
"""
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "app"
SRC = ROOT / "src/main/java/com/example/okane/data/local"
SCHEMA_4 = next((ROOT / "schemas").rglob("4.json"))
SCHEMA_5 = next((ROOT / "schemas").rglob("5.json"))

failures = []


def check(label, actual, expected):
    if actual == expected:
        print(f"  PASS  {label}")
    else:
        print(f"  FAIL  {label}\n          expected: {expected!r}\n          actual:   {actual!r}")
        failures.append(label)


# --- extract SQL from Kotlin -------------------------------------------------

def extract_exec_sql(kotlin: str, start: int, end: int):
    """All db.execSQL(...) statement strings within [start, end), in order."""
    body = kotlin[start:end]
    statements = []
    for m in re.finditer(r"db\.execSQL\(", body):
        i = m.end()
        depth = 1
        while depth:
            if body[i] == "(":
                depth += 1
            elif body[i] == ")":
                depth -= 1
            i += 1
        arg = body[m.end():i - 1]
        triple = re.search(r'"""(.*?)"""', arg, re.S)
        if triple:
            statements.append(triple.group(1))
        else:
            # Concatenated "..." + "..." literals.
            statements.append("".join(re.findall(r'"((?:[^"\\]|\\.)*)"', arg)))
    return statements


migrations_kt = (SRC / "Migrations.kt").read_text(encoding="utf-8")
m23_at = migrations_kt.index("val MIGRATION_2_3")
m34_at = migrations_kt.index("val MIGRATION_3_4")
m45_at = migrations_kt.index("val MIGRATION_4_5")
MIGRATION_2_3 = extract_exec_sql(migrations_kt, m23_at, m34_at)
MIGRATION_3_4 = extract_exec_sql(migrations_kt, m34_at, m45_at)
MIGRATION_4_5 = extract_exec_sql(migrations_kt, m45_at, len(migrations_kt))
print(f"extracted {len(MIGRATION_2_3)} statements for 2->3, {len(MIGRATION_3_4)} for 3->4,"
      f" {len(MIGRATION_4_5)} for 4->5")

wallet_dao_kt = (SRC / "WalletDao.kt").read_text(encoding="utf-8")
balance_queries = [q for q in re.findall(r'@Query\(\s*"""(.*?)"""', wallet_dao_kt, re.S)
                   if "initialBalance" in q]
BALANCE_ALL = next(q for q in balance_queries if "AS balance" in q)
BALANCE_ONE = next(q for q in balance_queries if "AS balance" not in q)
print("extracted observeBalances + observeBalance SQL\n")

# --- 1. v2 database ----------------------------------------------------------

db = sqlite3.connect(":memory:")
db.executescript(
    """
    CREATE TABLE IF NOT EXISTS `transactions` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `source` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL,
        `merchant` TEXT NOT NULL, `note` TEXT NOT NULL,
        `timestamp` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL);
    CREATE UNIQUE INDEX `index_transactions_source_type_amount_timestamp`
        ON `transactions` (`source`, `type`, `amount`, `timestamp`);
    CREATE TABLE IF NOT EXISTS `raw_notifications` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `packageName` TEXT NOT NULL, `title` TEXT, `text` TEXT, `bigText` TEXT,
        `postTime` INTEGER NOT NULL, `capturedAt` INTEGER NOT NULL);
    """
)
db.execute("INSERT INTO transactions (source,type,amount,merchant,note,timestamp,createdAt)"
           " VALUES ('GoPay','DEBIT',50000,'WARUNG BU SITI','raw',1751871000000,111)")
db.execute("INSERT INTO transactions (source,type,amount,merchant,note,timestamp,createdAt)"
           " VALUES ('Jenius','KREDIT',2500000,'Transfer masuk','raw2',1751784600000,222)")
db.execute("INSERT INTO raw_notifications (packageName,title,text,bigText,postTime,capturedAt)"
           " VALUES ('com.gojek.gopay','Top up berhasil','Top up Rp10.001 ...',NULL,1,2)")
db.commit()

print("running migrations 2 -> 3 -> 4")
for stmt in MIGRATION_2_3 + MIGRATION_3_4:
    db.execute(stmt)
db.commit()

# --- 2. schema equivalence vs Room's export ----------------------------------

# Compared via PRAGMA rather than SQL text: ALTER TABLE ... RENAME rewrites the
# stored DDL (quoting changes, whitespace is preserved verbatim), so string
# equality would flag cosmetic differences Room does not care about. Room
# validates columns, affinities, nullability, the primary key and the indices —
# so that is what is checked here.


def compare_schema(schema_path, version):
    print(f"\nschema vs Room schemas/{schema_path.name}")
    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    check(f"schema file is version {version}", schema["database"]["version"], version)
    expected_tables = sorted(e["tableName"] for e in schema["database"]["entities"])
    actual_tables = sorted(
        r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type = 'table'").fetchall()
        if not r[0].startswith("sqlite_") and r[0] != "room_master_table"
    )
    check(f"v{version} tables", actual_tables, expected_tables)
    for entity in schema["database"]["entities"]:
        compare_entity(entity)


def compare_entity(entity):
    table = entity["tableName"]

    expected_cols = [
        # Room omits notNull entirely for nullable columns.
        (f["columnName"], f["affinity"], 1 if f.get("notNull", False) else 0)
        for f in entity["fields"]
    ]
    actual_cols = [
        (r[1], r[2], r[3])
        for r in db.execute(f"PRAGMA table_info(`{table}`)").fetchall()
    ]
    check(f"table {table} columns", actual_cols, expected_cols)

    expected_pk = entity["primaryKey"]["columnNames"]
    actual_pk = [r[1] for r in db.execute(f"PRAGMA table_info(`{table}`)").fetchall() if r[5]]
    check(f"table {table} primary key", actual_pk, expected_pk)

    expected_ix = sorted(
        (ix["name"], 1 if ix["unique"] else 0, tuple(ix["columnNames"]))
        for ix in entity.get("indices", [])
    )
    actual_ix = sorted(
        (
            r[1],
            r[2],
            tuple(c[2] for c in db.execute(f"PRAGMA index_info(`{r[1]}`)").fetchall()),
        )
        for r in db.execute(f"PRAGMA index_list(`{table}`)").fetchall()
        if not r[1].startswith("sqlite_autoindex")
    )
    check(f"table {table} indices", actual_ix, expected_ix)


compare_schema(SCHEMA_4, 4)
# --- 3. data mapping ---------------------------------------------------------

print("\ndata preserved and remapped")
rows = db.execute("SELECT id, amount, type, walletId, toWalletId, categoryId, occurredAt,"
                  " createdAt, source, note, sourceApp, merchant FROM transactions"
                  " ORDER BY id").fetchall()
check("row count", len(rows), 2)
check("DEBIT -> OUT", rows[0][2], "OUT")
check("KREDIT -> IN", rows[1][2], "IN")
check("old source -> sourceApp", (rows[0][10], rows[1][10]), ("GoPay", "Jenius"))
check("new source = NOTIFICATION", rows[0][8], "NOTIFICATION")
check("timestamp -> occurredAt", rows[0][6], 1751871000000)
check("merchant kept", rows[0][11], "WARUNG BU SITI")
check("toWalletId null", rows[0][4], None)

default_wallet = db.execute("SELECT id, name, isDefault FROM wallets").fetchall()
check("default wallet created", default_wallet, [(1, "Tunai", 1)])
check("existing rows assigned to it", {r[3] for r in rows}, {1})
check("discovery samples survive",
      db.execute("SELECT COUNT(*) FROM raw_notifications").fetchone()[0], 1)
check("categories table empty (onboarding seeds it)",
      db.execute("SELECT COUNT(*) FROM categories").fetchone()[0], 0)

print("\nrunning migration 4 -> 5")
for stmt in MIGRATION_4_5:
    db.execute(stmt)
db.commit()
compare_schema(SCHEMA_5, 5)
check("4->5 leaves transactions untouched",
      db.execute("SELECT COUNT(*) FROM transactions").fetchone()[0], 2)
check("4->5 leaves discovery samples untouched",
      db.execute("SELECT COUNT(*) FROM raw_notifications").fetchone()[0], 1)

# --- 4. balance arithmetic ---------------------------------------------------

print("\nbalance query")
db.execute("DELETE FROM transactions")
db.execute("DELETE FROM wallets")
db.executescript(
    """
    INSERT INTO wallets (id,name,initialBalance,monthlyFee,isDefault,sortOrder,isArchived)
        VALUES (1,'Tunai',100000,0,1,0,0),
               (2,'BCA',1000000,0,0,1,0),
               (3,'OVO',5000,0,0,2,0),
               (4,'Lama',999999,0,0,3,1);
    """
)


def tx(amount, ttype, wallet, to_wallet=None):
    db.execute(
        "INSERT INTO transactions (amount,type,walletId,toWalletId,categoryId,"
        "occurredAt,createdAt,source,note,sourceApp,merchant)"
        " VALUES (?,?,?,?,NULL,1,1,'MANUAL',NULL,NULL,NULL)",
        (amount, ttype, wallet, to_wallet),
    )


tx(50000, "IN", 1)                 # Tunai +50k
tx(20000, "OUT", 1)                # Tunai -20k
tx(200000, "TRANSFER", 2, 1)       # BCA -> Tunai
tx(30000, "TRANSFER", 1, 2)        # Tunai -> BCA
tx(899999, "OUT", 4)               # archived wallet keeps a non-zero balance
db.commit()

result = db.execute(BALANCE_ALL).fetchall()
check("active and archived wallets included", [r[1] for r in result],
      ["Tunai", "BCA", "OVO", "Lama"])
# 100000 + 50000 - 20000 + 200000 (in) - 30000 (out)
check("transfers in both directions (Tunai)", result[0][6], 300000)
# 1000000 + 30000 (in) - 200000 (out)
check("transfers in both directions (BCA)", result[1][6], 830000)
# untouched wallet falls back to initialBalance, proving the IFNULLs
check("wallet with no transactions", result[2][6], 5000)
check("archived wallet balance retained", result[3][6], 100000)
check("isDefault surfaced", result[0][2], 1)
check("archived status surfaced", result[3][5], 1)

# A transfer must be net-zero across the two wallets it touches.
check("transfers net to zero overall", result[0][6] + result[1][6], 100000 + 1000000 + 30000)
check("all-wallet total includes archived", sum(r[6] for r in result), 1235000)

one = db.execute(BALANCE_ONE, {"walletId": 1}).fetchone()
check("single-wallet query agrees", one[0], 300000)

# --- 5. every DAO query against the final schema ------------------------------

print("\nDAO queries")


def dao_queries(path):
    """(name, sql) for each @Query in a DAO file, single-line or triple-quoted."""
    text = path.read_text(encoding="utf-8")
    found = []
    for m in re.finditer(r'@Query\(\s*(?:"""(.*?)"""|"((?:[^"\\]|\\.)*)")\s*\)\s*(?:suspend\s+)?fun\s+(\w+)', text, re.S):
        found.append((f"{path.stem}.{m.group(3)}", m.group(1) or m.group(2)))
    return found


queries = [q for dao in sorted(SRC.glob("*Dao.kt")) for q in dao_queries(dao)]
check("found DAO queries", len(queries) > 30, True)
for name, sql in queries:
    params = {p: 1 for p in re.findall(r":(\w+)", sql)}
    db.execute("SAVEPOINT probe")
    try:
        db.execute(sql, params).fetchall()
        ok = True
    except sqlite3.Error as e:
        ok = str(e)
    db.execute("ROLLBACK TO probe")
    db.execute("RELEASE probe")
    check(f"{name} runs", ok, True)

capture_dao = {name.split(".")[1]: sql for name, sql in queries if name.startswith("CaptureDao.")}
db.execute(
    "INSERT INTO capture_suggestions (sourceApp, packageName, channel, title, text, amount,"
    " direction, type, walletId, toWalletId, counterparty, postedAt, createdAt, status)"
    " VALUES ('Bale','com.google.android.apps.messaging','SMS',NULL,'Dana masuk Rp1',1,"
    " 'IN','IN',1,NULL,NULL,1000,1000,'PENDING')"
)
same = capture_dao["countSameSuggestion"]
check("NULL title matches NULL title",
      db.execute(same, {"sourceApp": "Bale", "title": None, "text": "Dana masuk Rp1",
                        "startMillis": 0, "endMillis": 2000}).fetchone()[0], 1)
check("a title does not match NULL",
      db.execute(same, {"sourceApp": "Bale", "title": "BTN", "text": "Dana masuk Rp1",
                        "startMillis": 0, "endMillis": 2000}).fetchone()[0], 0)
resolve = capture_dao["resolve"]
check("resolve changes a pending row",
      db.execute(resolve, {"id": 1, "status": "ACCEPTED", "transactionId": 9, "resolvedAt": 5}).rowcount, 1)
check("resolve leaves a resolved row alone",
      db.execute(resolve, {"id": 1, "status": "DISMISSED", "transactionId": None, "resolvedAt": 6}).rowcount, 0)

tx(70000, "OUT", 1)  # MANUAL
manual_id = db.execute("SELECT MAX(id) FROM transactions").fetchone()[0]
convert = capture_dao["convertToTransfer"]
check("a hand-typed row is never converted",
      db.execute(convert, {"id": manual_id, "fromWalletId": 1, "toWalletId": 2, "amount": 1}).rowcount, 0)
db.execute(
    "INSERT INTO transactions (amount,type,walletId,toWalletId,categoryId,occurredAt,createdAt,"
    "source,note,sourceApp,merchant) VALUES (101000,'OUT',2,NULL,NULL,7,7,'NOTIFICATION','n','Livin',NULL)"
)
note_id = db.execute("SELECT MAX(id) FROM transactions").fetchone()[0]
check("a notification row converts to a transfer",
      db.execute(convert, {"id": note_id, "fromWalletId": 2, "toWalletId": 3, "amount": 100000}).rowcount, 1)
check("converted row",
      db.execute("SELECT type, walletId, toWalletId, amount, categoryId FROM transactions WHERE id = ?",
                 (note_id,)).fetchone(), ("TRANSFER", 2, 3, 100000, None))
db.rollback()

print()
if failures:
    print(f"{len(failures)} FAILED: {failures}")
    sys.exit(1)
print("all SQL checks passed")
