ALTER TABLE expense_entries ADD COLUMN planner_bucket TEXT NOT NULL DEFAULT 'DISCRETIONARY';
ALTER TABLE expense_entries ADD COLUMN recurring INTEGER NOT NULL DEFAULT 0;

UPDATE expense_entries
SET planner_bucket = CASE category
    WHEN 'FOOD' THEN 'FOOD'
    WHEN 'BILLS' THEN 'FIXED_COSTS'
    WHEN 'SUBSCRIPTIONS' THEN 'FIXED_COSTS'
    WHEN 'CAR' THEN 'TRANSPORT'
    WHEN 'FAMILY' THEN 'FAMILY'
    ELSE 'DISCRETIONARY'
END
WHERE planner_bucket IS NULL
   OR planner_bucket = ''
   OR planner_bucket = 'DISCRETIONARY';

CREATE INDEX IF NOT EXISTS idx_expense_entries_month_planner_bucket
    ON expense_entries (month, planner_bucket);
