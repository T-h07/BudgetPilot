CREATE INDEX IF NOT EXISTS idx_income_entries_month_received_date ON income_entries (month, received_date);
CREATE INDEX IF NOT EXISTS idx_expense_entries_month_expense_date ON expense_entries (month, expense_date);
CREATE INDEX IF NOT EXISTS idx_expense_entries_tag ON expense_entries (tag);
CREATE INDEX IF NOT EXISTS idx_savings_entries_bucket_month_date ON savings_entries (bucket_id, month, entry_date);
CREATE INDEX IF NOT EXISTS idx_goal_contributions_goal_month_date ON goal_contributions (goal_id, month, contribution_date);
CREATE INDEX IF NOT EXISTS idx_family_expenses_member_month_date ON family_expense_entries (family_member_id, month, expense_date);
CREATE INDEX IF NOT EXISTS idx_investment_transactions_investment_month_date ON investment_transactions (investment_id, month, transaction_date);
