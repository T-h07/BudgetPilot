CREATE TABLE IF NOT EXISTS expense_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    planner_bucket TEXT NOT NULL,
    category TEXT NOT NULL,
    subcategory TEXT,
    payment_method TEXT,
    default_amount TEXT NOT NULL,
    cadence TEXT NOT NULL,
    day_of_month INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    tag TEXT,
    note TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS income_templates (
    id TEXT PRIMARY KEY,
    source_name TEXT NOT NULL,
    income_type TEXT NOT NULL,
    default_amount TEXT NOT NULL,
    cadence TEXT NOT NULL,
    day_of_month INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    note TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

ALTER TABLE expense_entries ADD COLUMN source_template_id TEXT;
ALTER TABLE income_entries ADD COLUMN source_template_id TEXT;

CREATE INDEX IF NOT EXISTS idx_expense_templates_active_cadence
    ON expense_templates (active, cadence);
CREATE INDEX IF NOT EXISTS idx_income_templates_active_cadence
    ON income_templates (active, cadence);

CREATE INDEX IF NOT EXISTS idx_expense_entries_month_source_template
    ON expense_entries (month, source_template_id);
CREATE INDEX IF NOT EXISTS idx_income_entries_month_source_template
    ON income_entries (month, source_template_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_expense_entries_month_source_template
    ON expense_entries (month, source_template_id)
    WHERE source_template_id IS NOT NULL AND source_template_id <> '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_income_entries_month_source_template
    ON income_entries (month, source_template_id)
    WHERE source_template_id IS NOT NULL AND source_template_id <> '';
