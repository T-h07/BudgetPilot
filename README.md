# BudgetPilot

**BudgetPilot** is a JavaFX desktop app that acts like a **monthly financial cockpit**: you plan your month, track spending, forecast outcomes, build savings, fund goals, control habits, and monitor progress — all from a clean, modern dashboard.

> **Status:** Active development  
> **Tech:** Java 21 • JavaFX • Maven • SQLite (local persistence)

---

## Why BudgetPilot

Most “expense trackers” only tell you what you already did.

BudgetPilot is built around a monthly loop:

1. **Plan** your month (income + budgets)
2. **Track** expenses (categorized + budget-bucketed)
3. **Forecast** month-end results (pace-based projection)
4. **Progress** with Savings + Goals
5. **Improve behavior** with Habits + Insights
6. **Grow** with Investments
7. **Stay motivated** with Achievements

---

## Features

### Dashboard (Command Center)
- KPI tiles: remaining money, spending, income, forecast, savings/goals progress, burn rate, budget health
- Spending by category panel
- Weekly spending trend
- Forecast + alerts/insights

### Planner (Monthly Budget)
- Set budgets for: **Fixed Costs, Food, Transport, Family, Discretionary**
- Planned savings/goals % (for guidance + comparison)
- Plan vs Actual reporting (bucket-level)

### Expenses
- Add/edit/delete expenses with:
  - amount, date, category, payment method, tag, note
  - **Planner Bucket** mapping (Fixed/Food/Transport/Family/Discretionary/Unplanned)
  - One-time/unplanned purchases supported
- Filters + search
- Category breakdown + analytics
- Forecast integration

### Income
- Add/edit/delete monthly income sources
- Track planned vs received
- Summary metrics (planned/received/recurring)

### Savings
- Savings buckets (Emergency Fund, Tuition, etc.)
- Transactions: contribute / withdraw
- Monthly contribution tracking + totals

### Goals
- Create goals (e.g., Car, Laptop)
- Contribute consistently and track progress
- Progress metrics + estimated completion (basic)

### Habits
- Define rules like **#snacks**, **#clothes**, etc.
- Monthly limits + warning thresholds
- Evaluates real spending and flags warning/exceeded habits

### Family (optional module)
- Track dependents/support: allowances, medical, support costs
- Monthly family summaries and budget comparison

### Investments (optional module)
- Track financial & personal investments
- Contributions/returns and ROI-style summaries

### Achievements (optional module)
- Computed achievement system based on your activity
- Progress tracking + milestone motivation

### Persistence + Backup
- Local SQLite persistence (schema migrations)
- Backup export/import (if enabled in your branch)

---

## Screenshots
> Add screenshots here (recommended):
- Dashboard
- Expenses
- Planner (Plan vs Actual)
- Savings/Goals

---

## Getting Started

### Prerequisites
- **Java 21**
- **Maven**
- JavaFX dependencies are handled via Maven.

### Run the app
```bash
mvn javafx:run
