ALTER TABLE monthly_plans ADD COLUMN habit_percent TEXT NOT NULL DEFAULT '10.00';
ALTER TABLE monthly_plans ADD COLUMN habit_mode TEXT NOT NULL DEFAULT 'DYNAMIC';

UPDATE monthly_plans
SET habit_percent = '10.00'
WHERE habit_percent IS NULL
   OR trim(habit_percent) = '';

UPDATE monthly_plans
SET habit_mode = 'DYNAMIC'
WHERE habit_mode IS NULL
   OR trim(habit_mode) = '';

ALTER TABLE habit_rules ADD COLUMN enabled_this_month INTEGER NOT NULL DEFAULT 1;
ALTER TABLE habit_rules ADD COLUMN weight INTEGER NOT NULL DEFAULT 1;

UPDATE habit_rules
SET enabled_this_month = 1
WHERE enabled_this_month IS NULL;

UPDATE habit_rules
SET weight = 1
WHERE weight IS NULL
   OR weight < 1;

UPDATE habit_rules
SET weight = 10
WHERE weight > 10;
