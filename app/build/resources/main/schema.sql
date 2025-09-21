PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS budget (
  id INTEGER PRIMARY KEY,
  month TEXT NOT NULL,             -- YYYY-MM
  cap_cents INTEGER NOT NULL,
  UNIQUE(month) ON CONFLICT REPLACE
);

-- renamed from "transaction" (reserved word) -> "tx"
CREATE TABLE IF NOT EXISTS tx (
  id INTEGER PRIMARY KEY,
  date TEXT NOT NULL,              -- yyyy-mm-dd
  description TEXT,
  amount_cents INTEGER NOT NULL    -- negative = expense, positive = income
);

-- Recurring monthly amounts (we'll convert yearly -> monthly when saving)
CREATE TABLE IF NOT EXISTS recurring (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,               -- e.g., Tuition, Rent, Scholarship
  kind TEXT NOT NULL,               -- 'expense' or 'income'
  amount_cents INTEGER NOT NULL     -- monthly amount in cents (+ always)
);
