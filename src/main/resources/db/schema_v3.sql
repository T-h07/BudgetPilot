ALTER TABLE savings_entries ADD COLUMN related_goal_id TEXT;
ALTER TABLE goal_contributions ADD COLUMN source_type TEXT NOT NULL DEFAULT 'FREE_MONEY';
ALTER TABLE goal_contributions ADD COLUMN source_ref_id TEXT;

CREATE INDEX IF NOT EXISTS idx_savings_entries_related_goal ON savings_entries (related_goal_id);
CREATE INDEX IF NOT EXISTS idx_goal_contributions_source_type ON goal_contributions (source_type);
