PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS user_profile (
    singleton_key INTEGER PRIMARY KEY CHECK (singleton_key = 1),
    id TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL,
    age INTEGER,
    profile_type TEXT NOT NULL,
    currency_code TEXT NOT NULL,
    family_module_enabled INTEGER NOT NULL,
    investments_module_enabled INTEGER NOT NULL,
    achievements_module_enabled INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY,
    value TEXT,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS monthly_plans (
    month TEXT PRIMARY KEY,
    id TEXT NOT NULL,
    fixed_costs_budget TEXT NOT NULL,
    food_budget TEXT NOT NULL,
    transport_budget TEXT NOT NULL,
    family_budget TEXT NOT NULL,
    discretionary_budget TEXT NOT NULL,
    savings_percent TEXT NOT NULL,
    goals_percent TEXT NOT NULL,
    safety_buffer_amount TEXT NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS income_entries (
    id TEXT PRIMARY KEY,
    month TEXT NOT NULL,
    received_date TEXT NOT NULL,
    source_name TEXT NOT NULL,
    income_type TEXT NOT NULL,
    amount TEXT NOT NULL,
    recurring INTEGER NOT NULL,
    received INTEGER NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS expense_entries (
    id TEXT PRIMARY KEY,
    month TEXT NOT NULL,
    expense_date TEXT NOT NULL,
    amount TEXT NOT NULL,
    category TEXT NOT NULL,
    subcategory TEXT,
    note TEXT,
    payment_method TEXT NOT NULL,
    tag TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS savings_buckets (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    current_amount TEXT NOT NULL,
    target_amount TEXT,
    active INTEGER NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS savings_entries (
    id TEXT PRIMARY KEY,
    bucket_id TEXT NOT NULL,
    month TEXT NOT NULL,
    entry_date TEXT NOT NULL,
    amount TEXT NOT NULL,
    entry_type TEXT NOT NULL,
    note TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (bucket_id) REFERENCES savings_buckets (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS goals (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    goal_type TEXT NOT NULL,
    target_amount TEXT NOT NULL,
    current_amount TEXT NOT NULL,
    target_date TEXT,
    priority INTEGER NOT NULL,
    active INTEGER NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS goal_contributions (
    id TEXT PRIMARY KEY,
    goal_id TEXT NOT NULL,
    month TEXT NOT NULL,
    contribution_date TEXT NOT NULL,
    amount TEXT NOT NULL,
    type TEXT NOT NULL,
    note TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS family_members (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    relationship_type TEXT NOT NULL,
    weekly_allowance TEXT NOT NULL,
    monthly_medical_budget TEXT NOT NULL,
    monthly_support_budget TEXT NOT NULL,
    active INTEGER NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS family_expense_entries (
    id TEXT PRIMARY KEY,
    family_member_id TEXT NOT NULL,
    month TEXT NOT NULL,
    expense_date TEXT NOT NULL,
    amount TEXT NOT NULL,
    expense_type TEXT NOT NULL,
    note TEXT,
    related_expense_entry_id TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (family_member_id) REFERENCES family_members (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS habit_rules (
    id TEXT PRIMARY KEY,
    tag TEXT NOT NULL,
    display_name TEXT NOT NULL,
    linked_category TEXT,
    monthly_limit TEXT NOT NULL,
    warning_threshold_percent TEXT NOT NULL,
    active INTEGER NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS investments (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    kind TEXT NOT NULL,
    status TEXT NOT NULL,
    target_amount TEXT,
    current_invested_amount TEXT NOT NULL,
    current_estimated_value TEXT NOT NULL,
    expected_profit_amount TEXT,
    expected_profit_percent TEXT,
    start_date TEXT NOT NULL,
    expected_return_date TEXT,
    priority INTEGER NOT NULL,
    active INTEGER NOT NULL,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS investment_transactions (
    id TEXT PRIMARY KEY,
    investment_id TEXT NOT NULL,
    month TEXT NOT NULL,
    transaction_date TEXT NOT NULL,
    amount TEXT NOT NULL,
    type TEXT NOT NULL,
    note TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (investment_id) REFERENCES investments (id) ON DELETE CASCADE
);
