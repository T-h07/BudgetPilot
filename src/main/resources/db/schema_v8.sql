ALTER TABLE habit_rules ADD COLUMN baseline_amount TEXT NOT NULL DEFAULT '0.00';

UPDATE habit_rules
SET baseline_amount = '0.00'
WHERE baseline_amount IS NULL
   OR trim(baseline_amount) = '';
