package com.budgetpilot.store;

import com.budgetpilot.model.ExpenseEntry;
import com.budgetpilot.model.FamilyExpenseEntry;
import com.budgetpilot.model.FamilyMember;
import com.budgetpilot.model.Goal;
import com.budgetpilot.model.GoalContribution;
import com.budgetpilot.model.HabitRule;
import com.budgetpilot.model.IncomeEntry;
import com.budgetpilot.model.Investment;
import com.budgetpilot.model.InvestmentTransaction;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.model.SavingsBucket;
import com.budgetpilot.model.SavingsEntry;
import com.budgetpilot.model.UserProfile;
import com.budgetpilot.model.enums.ExpenseCategory;
import com.budgetpilot.model.enums.FamilyExpenseType;
import com.budgetpilot.model.enums.GoalContributionType;
import com.budgetpilot.model.enums.GoalType;
import com.budgetpilot.model.enums.IncomeType;
import com.budgetpilot.model.enums.InvestmentKind;
import com.budgetpilot.model.enums.InvestmentStatus;
import com.budgetpilot.model.enums.InvestmentTransactionType;
import com.budgetpilot.model.enums.InvestmentType;
import com.budgetpilot.model.enums.PaymentMethod;
import com.budgetpilot.model.enums.RelationshipType;
import com.budgetpilot.model.enums.SavingsEntryType;
import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.persistence.DbManager;
import com.budgetpilot.persistence.DbUtils;
import com.budgetpilot.util.MonthUtils;
import com.budgetpilot.util.ValidationUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public class DbStore extends InMemoryStore implements AutoCloseable {
    private final Connection connection;
    private boolean persistenceSuspended;

    public DbStore(Connection connection) {
        this.connection = ValidationUtils.requireNonNull(connection, "connection");
        loadFromDatabase();
    }

    public synchronized int getSchemaVersion() {
        try {
            return DbManager.readUserVersion(connection);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed reading schema version", ex);
        }
    }

    @Override
    public synchronized void saveUserProfile(UserProfile profile) {
        persistWith(() -> super.saveUserProfile(profile));
    }

    @Override
    public synchronized void saveMonthlyPlan(MonthlyPlan plan) {
        persistWith(() -> super.saveMonthlyPlan(plan));
    }

    @Override
    public synchronized void saveIncomeEntry(IncomeEntry entry) {
        persistWith(() -> super.saveIncomeEntry(entry));
    }

    @Override
    public synchronized void deleteIncomeEntry(String id) {
        persistWith(() -> super.deleteIncomeEntry(id));
    }

    @Override
    public synchronized void saveExpenseEntry(ExpenseEntry entry) {
        persistWith(() -> super.saveExpenseEntry(entry));
    }

    @Override
    public synchronized void deleteExpenseEntry(String id) {
        persistWith(() -> super.deleteExpenseEntry(id));
    }

    @Override
    public synchronized void saveSavingsBucket(SavingsBucket bucket) {
        persistWith(() -> super.saveSavingsBucket(bucket));
    }

    @Override
    public synchronized void deleteSavingsBucket(String id) {
        persistWith(() -> super.deleteSavingsBucket(id));
    }

    @Override
    public synchronized void saveSavingsEntry(SavingsEntry entry) {
        persistWith(() -> super.saveSavingsEntry(entry));
    }

    @Override
    public synchronized void deleteSavingsEntry(String id) {
        persistWith(() -> super.deleteSavingsEntry(id));
    }

    @Override
    public synchronized void saveGoal(Goal goal) {
        persistWith(() -> super.saveGoal(goal));
    }

    @Override
    public synchronized void deleteGoal(String id) {
        persistWith(() -> super.deleteGoal(id));
    }

    @Override
    public synchronized void saveGoalContribution(GoalContribution entry) {
        persistWith(() -> super.saveGoalContribution(entry));
    }

    @Override
    public synchronized void deleteGoalContribution(String id) {
        persistWith(() -> super.deleteGoalContribution(id));
    }

    @Override
    public synchronized void saveFamilyMember(FamilyMember member) {
        persistWith(() -> super.saveFamilyMember(member));
    }

    @Override
    public synchronized void deleteFamilyMember(String id) {
        persistWith(() -> super.deleteFamilyMember(id));
    }

    @Override
    public synchronized void saveFamilyExpenseEntry(FamilyExpenseEntry entry) {
        persistWith(() -> super.saveFamilyExpenseEntry(entry));
    }

    @Override
    public synchronized void deleteFamilyExpenseEntry(String id) {
        persistWith(() -> super.deleteFamilyExpenseEntry(id));
    }

    @Override
    public synchronized void saveHabitRule(HabitRule rule) {
        persistWith(() -> super.saveHabitRule(rule));
    }

    @Override
    public synchronized void deleteHabitRule(String id) {
        persistWith(() -> super.deleteHabitRule(id));
    }

    @Override
    public synchronized void saveInvestment(Investment investment) {
        persistWith(() -> super.saveInvestment(investment));
    }

    @Override
    public synchronized void deleteInvestment(String id) {
        persistWith(() -> super.deleteInvestment(id));
    }

    @Override
    public synchronized void saveInvestmentTransaction(InvestmentTransaction tx) {
        persistWith(() -> super.saveInvestmentTransaction(tx));
    }

    @Override
    public synchronized void deleteInvestmentTransaction(String id) {
        persistWith(() -> super.deleteInvestmentTransaction(id));
    }

    @Override
    public synchronized void saveAppSetting(String key, String value) {
        persistWith(() -> super.saveAppSetting(key, value));
    }

    @Override
    public synchronized void deleteAppSetting(String key) {
        persistWith(() -> super.deleteAppSetting(key));
    }

    @Override
    public synchronized void clearAll() {
        persistWith(super::clearAll);
    }

    @Override
    public synchronized void close() throws Exception {
        connection.close();
    }

    private void persistWith(Runnable action) {
        action.run();
        if (!persistenceSuspended) {
            writeSnapshotToDatabase();
        }
    }

    public synchronized void runBulkUpdate(Runnable action) {
        ValidationUtils.requireNonNull(action, "action");
        boolean previous = persistenceSuspended;
        persistenceSuspended = true;
        try {
            action.run();
        } finally {
            persistenceSuspended = previous;
        }
        writeSnapshotToDatabase();
    }

    private void loadFromDatabase() {
        super.clearAll();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT key, value FROM app_settings")) {
            while (rs.next()) {
                super.saveAppSetting(rs.getString("key"), rs.getString("value"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading app_settings", ex);
        }

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM user_profile WHERE singleton_key = 1 LIMIT 1")) {
            if (rs.next()) {
                UserProfile profile = new UserProfile();
                profile.setId(rs.getString("id"));
                profile.setFirstName(rs.getString("first_name"));
                profile.setLastName(rs.getString("last_name"));
                profile.setEmail(rs.getString("email"));
                Object ageObject = rs.getObject("age");
                profile.setAge(ageObject == null ? null : rs.getInt("age"));
                profile.setProfileType(DbUtils.parseEnum(rs.getString("profile_type"), UserProfileType.class, UserProfileType.PERSONAL_USE, "user_profile.profile_type"));
                profile.setCurrencyCode(rs.getString("currency_code"));
                profile.setFamilyModuleEnabled(DbUtils.fromIntBoolean(rs.getInt("family_module_enabled")));
                profile.setInvestmentsModuleEnabled(DbUtils.fromIntBoolean(rs.getInt("investments_module_enabled")));
                profile.setAchievementsModuleEnabled(DbUtils.fromIntBoolean(rs.getInt("achievements_module_enabled")));
                profile.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "user_profile.created_at"));
                profile.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "user_profile.updated_at"));
                super.saveUserProfile(profile);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading user_profile", ex);
        }

        loadMonthlyPlans();
        loadIncomeEntries();
        loadExpenseEntries();
        loadSavingsBuckets();
        loadSavingsEntries();
        loadGoals();
        loadGoalContributions();
        loadFamilyMembers();
        loadFamilyExpenseEntries();
        loadHabitRules();
        loadInvestments();
        loadInvestmentTransactions();
    }

    private void loadMonthlyPlans() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM monthly_plans")) {
            while (rs.next()) {
                MonthlyPlan plan = new MonthlyPlan();
                plan.setId(rs.getString("id"));
                plan.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "monthly_plans.month"));
                plan.setFixedCostsBudget(DbUtils.parseBigDecimal(rs.getString("fixed_costs_budget"), "monthly_plans.fixed_costs_budget", BigDecimal.ZERO));
                plan.setFoodBudget(DbUtils.parseBigDecimal(rs.getString("food_budget"), "monthly_plans.food_budget", BigDecimal.ZERO));
                plan.setTransportBudget(DbUtils.parseBigDecimal(rs.getString("transport_budget"), "monthly_plans.transport_budget", BigDecimal.ZERO));
                plan.setFamilyBudget(DbUtils.parseBigDecimal(rs.getString("family_budget"), "monthly_plans.family_budget", BigDecimal.ZERO));
                plan.setDiscretionaryBudget(DbUtils.parseBigDecimal(rs.getString("discretionary_budget"), "monthly_plans.discretionary_budget", BigDecimal.ZERO));
                plan.setSavingsPercent(DbUtils.parseBigDecimal(rs.getString("savings_percent"), "monthly_plans.savings_percent", BigDecimal.ZERO));
                plan.setGoalsPercent(DbUtils.parseBigDecimal(rs.getString("goals_percent"), "monthly_plans.goals_percent", BigDecimal.ZERO));
                plan.setSafetyBufferAmount(DbUtils.parseBigDecimal(rs.getString("safety_buffer_amount"), "monthly_plans.safety_buffer_amount", BigDecimal.ZERO));
                plan.setNotes(rs.getString("notes"));
                plan.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "monthly_plans.created_at"));
                plan.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "monthly_plans.updated_at"));
                super.saveMonthlyPlan(plan);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading monthly_plans", ex);
        }
    }

    private void loadIncomeEntries() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM income_entries")) {
            while (rs.next()) {
                IncomeEntry entry = new IncomeEntry();
                entry.setId(rs.getString("id"));
                entry.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "income_entries.month"));
                entry.setReceivedDate(DbUtils.parseLocalDate(rs.getString("received_date"), LocalDate.now(), "income_entries.received_date"));
                entry.setSourceName(rs.getString("source_name"));
                entry.setIncomeType(DbUtils.parseEnum(rs.getString("income_type"), IncomeType.class, IncomeType.OTHER, "income_entries.income_type"));
                entry.setAmount(DbUtils.parseBigDecimal(rs.getString("amount"), "income_entries.amount", BigDecimal.ZERO));
                entry.setRecurring(DbUtils.fromIntBoolean(rs.getInt("recurring")));
                entry.setReceived(DbUtils.fromIntBoolean(rs.getInt("received")));
                entry.setNotes(rs.getString("notes"));
                entry.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "income_entries.created_at"));
                entry.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "income_entries.updated_at"));
                super.saveIncomeEntry(entry);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading income_entries", ex);
        }
    }

    private void loadExpenseEntries() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM expense_entries")) {
            while (rs.next()) {
                ExpenseEntry entry = new ExpenseEntry();
                entry.setId(rs.getString("id"));
                entry.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "expense_entries.month"));
                entry.setExpenseDate(DbUtils.parseLocalDate(rs.getString("expense_date"), LocalDate.now(), "expense_entries.expense_date"));
                entry.setAmount(DbUtils.parseBigDecimal(rs.getString("amount"), "expense_entries.amount", BigDecimal.ONE));
                entry.setCategory(DbUtils.parseEnum(rs.getString("category"), ExpenseCategory.class, ExpenseCategory.OTHER, "expense_entries.category"));
                entry.setSubcategory(rs.getString("subcategory"));
                entry.setNote(rs.getString("note"));
                entry.setPaymentMethod(DbUtils.parseEnum(rs.getString("payment_method"), PaymentMethod.class, PaymentMethod.CARD, "expense_entries.payment_method"));
                entry.setTag(rs.getString("tag"));
                entry.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "expense_entries.created_at"));
                entry.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "expense_entries.updated_at"));
                super.saveExpenseEntry(entry);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading expense_entries", ex);
        }
    }

    private void loadSavingsBuckets() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM savings_buckets")) {
            while (rs.next()) {
                SavingsBucket bucket = new SavingsBucket();
                bucket.setId(rs.getString("id"));
                bucket.setName(rs.getString("name"));
                bucket.setCurrentAmount(DbUtils.parseBigDecimal(rs.getString("current_amount"), "savings_buckets.current_amount", BigDecimal.ZERO));
                bucket.setTargetAmount(DbUtils.parseNullableBigDecimal(rs.getString("target_amount")));
                bucket.setActive(DbUtils.fromIntBoolean(rs.getInt("active")));
                bucket.setNotes(rs.getString("notes"));
                bucket.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "savings_buckets.created_at"));
                bucket.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "savings_buckets.updated_at"));
                super.saveSavingsBucket(bucket);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading savings_buckets", ex);
        }
    }

    private void loadSavingsEntries() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM savings_entries")) {
            while (rs.next()) {
                SavingsEntry entry = new SavingsEntry();
                entry.setId(rs.getString("id"));
                entry.setBucketId(rs.getString("bucket_id"));
                entry.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "savings_entries.month"));
                entry.setEntryDate(DbUtils.parseLocalDate(rs.getString("entry_date"), LocalDate.now(), "savings_entries.entry_date"));
                entry.setAmount(DbUtils.parseBigDecimal(rs.getString("amount"), "savings_entries.amount", BigDecimal.ONE));
                entry.setEntryType(DbUtils.parseEnum(rs.getString("entry_type"), SavingsEntryType.class, SavingsEntryType.CONTRIBUTION, "savings_entries.entry_type"));
                entry.setNote(rs.getString("note"));
                entry.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "savings_entries.created_at"));
                entry.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "savings_entries.updated_at"));
                super.saveSavingsEntry(entry);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading savings_entries", ex);
        }
    }

    private void loadGoals() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM goals")) {
            while (rs.next()) {
                Goal goal = new Goal();
                goal.setId(rs.getString("id"));
                goal.setName(rs.getString("name"));
                goal.setGoalType(DbUtils.parseEnum(rs.getString("goal_type"), GoalType.class, GoalType.CUSTOM, "goals.goal_type"));
                goal.setTargetAmount(DbUtils.parseBigDecimal(rs.getString("target_amount"), "goals.target_amount", BigDecimal.ZERO));
                goal.setCurrentAmount(DbUtils.parseBigDecimal(rs.getString("current_amount"), "goals.current_amount", BigDecimal.ZERO));
                goal.setTargetDate(DbUtils.parseLocalDate(rs.getString("target_date"), null, "goals.target_date"));
                goal.setPriority(rs.getInt("priority"));
                goal.setActive(DbUtils.fromIntBoolean(rs.getInt("active")));
                goal.setNotes(rs.getString("notes"));
                goal.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "goals.created_at"));
                goal.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "goals.updated_at"));
                super.saveGoal(goal);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading goals", ex);
        }
    }

    private void loadGoalContributions() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM goal_contributions")) {
            while (rs.next()) {
                GoalContribution entry = new GoalContribution();
                entry.setId(rs.getString("id"));
                entry.setGoalId(rs.getString("goal_id"));
                entry.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "goal_contributions.month"));
                entry.setContributionDate(DbUtils.parseLocalDate(rs.getString("contribution_date"), LocalDate.now(), "goal_contributions.contribution_date"));
                entry.setAmount(DbUtils.parseBigDecimal(rs.getString("amount"), "goal_contributions.amount", BigDecimal.ONE));
                entry.setType(DbUtils.parseEnum(rs.getString("type"), GoalContributionType.class, GoalContributionType.CONTRIBUTION, "goal_contributions.type"));
                entry.setNote(rs.getString("note"));
                entry.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "goal_contributions.created_at"));
                entry.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "goal_contributions.updated_at"));
                super.saveGoalContribution(entry);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading goal_contributions", ex);
        }
    }

    private void loadFamilyMembers() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM family_members")) {
            while (rs.next()) {
                FamilyMember member = new FamilyMember();
                member.setId(rs.getString("id"));
                member.setName(rs.getString("name"));
                member.setRelationshipType(DbUtils.parseEnum(rs.getString("relationship_type"), RelationshipType.class, RelationshipType.OTHER, "family_members.relationship_type"));
                member.setWeeklyAllowance(DbUtils.parseBigDecimal(rs.getString("weekly_allowance"), "family_members.weekly_allowance", BigDecimal.ZERO));
                member.setMonthlyMedicalBudget(DbUtils.parseBigDecimal(rs.getString("monthly_medical_budget"), "family_members.monthly_medical_budget", BigDecimal.ZERO));
                member.setMonthlySupportBudget(DbUtils.parseBigDecimal(rs.getString("monthly_support_budget"), "family_members.monthly_support_budget", BigDecimal.ZERO));
                member.setActive(DbUtils.fromIntBoolean(rs.getInt("active")));
                member.setNotes(rs.getString("notes"));
                member.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "family_members.created_at"));
                member.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "family_members.updated_at"));
                super.saveFamilyMember(member);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading family_members", ex);
        }
    }

    private void loadFamilyExpenseEntries() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM family_expense_entries")) {
            while (rs.next()) {
                FamilyExpenseEntry entry = new FamilyExpenseEntry();
                entry.setId(rs.getString("id"));
                entry.setFamilyMemberId(rs.getString("family_member_id"));
                entry.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "family_expense_entries.month"));
                entry.setExpenseDate(DbUtils.parseLocalDate(rs.getString("expense_date"), LocalDate.now(), "family_expense_entries.expense_date"));
                entry.setAmount(DbUtils.parseBigDecimal(rs.getString("amount"), "family_expense_entries.amount", BigDecimal.ONE));
                entry.setExpenseType(DbUtils.parseEnum(rs.getString("expense_type"), FamilyExpenseType.class, FamilyExpenseType.SUPPORT, "family_expense_entries.expense_type"));
                entry.setNote(rs.getString("note"));
                entry.setRelatedExpenseEntryId(rs.getString("related_expense_entry_id"));
                entry.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "family_expense_entries.created_at"));
                entry.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "family_expense_entries.updated_at"));
                super.saveFamilyExpenseEntry(entry);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading family_expense_entries", ex);
        }
    }

    private void loadHabitRules() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM habit_rules")) {
            while (rs.next()) {
                HabitRule rule = new HabitRule();
                rule.setId(rs.getString("id"));
                rule.setTag(rs.getString("tag"));
                rule.setDisplayName(rs.getString("display_name"));
                rule.setLinkedCategory(DbUtils.parseEnum(rs.getString("linked_category"), ExpenseCategory.class, null, "habit_rules.linked_category"));
                rule.setMonthlyLimit(DbUtils.parseBigDecimal(rs.getString("monthly_limit"), "habit_rules.monthly_limit", BigDecimal.ZERO));
                rule.setWarningThresholdPercent(DbUtils.parseBigDecimal(rs.getString("warning_threshold_percent"), "habit_rules.warning_threshold_percent", BigDecimal.ZERO));
                rule.setActive(DbUtils.fromIntBoolean(rs.getInt("active")));
                rule.setNotes(rs.getString("notes"));
                rule.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "habit_rules.created_at"));
                rule.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "habit_rules.updated_at"));
                super.saveHabitRule(rule);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading habit_rules", ex);
        }
    }

    private void loadInvestments() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM investments")) {
            while (rs.next()) {
                Investment investment = new Investment();
                investment.setId(rs.getString("id"));
                investment.setName(rs.getString("name"));
                investment.setType(DbUtils.parseEnum(rs.getString("type"), InvestmentType.class, InvestmentType.OTHER, "investments.type"));
                investment.setKind(DbUtils.parseEnum(rs.getString("kind"), InvestmentKind.class, InvestmentKind.MONEY, "investments.kind"));
                investment.setStatus(DbUtils.parseEnum(rs.getString("status"), InvestmentStatus.class, InvestmentStatus.PLANNED, "investments.status"));
                investment.setTargetAmount(DbUtils.parseNullableBigDecimal(rs.getString("target_amount")));
                investment.setCurrentInvestedAmount(DbUtils.parseBigDecimal(rs.getString("current_invested_amount"), "investments.current_invested_amount", BigDecimal.ZERO));
                investment.setCurrentEstimatedValue(DbUtils.parseBigDecimal(rs.getString("current_estimated_value"), "investments.current_estimated_value", BigDecimal.ZERO));
                investment.setExpectedProfitAmount(DbUtils.parseNullableBigDecimal(rs.getString("expected_profit_amount")));
                investment.setExpectedProfitPercent(DbUtils.parseNullableBigDecimal(rs.getString("expected_profit_percent")));
                investment.setStartDate(DbUtils.parseLocalDate(rs.getString("start_date"), LocalDate.now(), "investments.start_date"));
                investment.setExpectedReturnDate(DbUtils.parseLocalDate(rs.getString("expected_return_date"), null, "investments.expected_return_date"));
                investment.setPriority(rs.getInt("priority"));
                investment.setActive(DbUtils.fromIntBoolean(rs.getInt("active")));
                investment.setNotes(rs.getString("notes"));
                investment.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "investments.created_at"));
                investment.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "investments.updated_at"));
                super.saveInvestment(investment);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading investments", ex);
        }
    }

    private void loadInvestmentTransactions() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM investment_transactions")) {
            while (rs.next()) {
                InvestmentTransaction tx = new InvestmentTransaction();
                tx.setId(rs.getString("id"));
                tx.setInvestmentId(rs.getString("investment_id"));
                tx.setMonth(DbUtils.parseYearMonth(rs.getString("month"), MonthUtils.currentMonth(), "investment_transactions.month"));
                tx.setTransactionDate(DbUtils.parseLocalDate(rs.getString("transaction_date"), LocalDate.now(), "investment_transactions.transaction_date"));
                tx.setAmount(DbUtils.parseBigDecimal(rs.getString("amount"), "investment_transactions.amount", BigDecimal.ONE));
                tx.setType(DbUtils.parseEnum(rs.getString("type"), InvestmentTransactionType.class, InvestmentTransactionType.CONTRIBUTION, "investment_transactions.type"));
                tx.setNote(rs.getString("note"));
                tx.setCreatedAt(DbUtils.parseLocalDateTime(rs.getString("created_at"), LocalDateTime.now(), "investment_transactions.created_at"));
                tx.setUpdatedAt(DbUtils.parseLocalDateTime(rs.getString("updated_at"), LocalDateTime.now(), "investment_transactions.updated_at"));
                super.saveInvestmentTransaction(tx);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed loading investment_transactions", ex);
        }
    }

    private void writeSnapshotToDatabase() {
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                clearTables();
                insertAllData();
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed persisting to SQLite", ex);
        }
    }

    private void clearTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM user_profile");
            statement.executeUpdate("DELETE FROM monthly_plans");
            statement.executeUpdate("DELETE FROM income_entries");
            statement.executeUpdate("DELETE FROM expense_entries");
            statement.executeUpdate("DELETE FROM savings_buckets");
            statement.executeUpdate("DELETE FROM goals");
            statement.executeUpdate("DELETE FROM family_members");
            statement.executeUpdate("DELETE FROM habit_rules");
            statement.executeUpdate("DELETE FROM investments");
            statement.executeUpdate("DELETE FROM app_settings");
        }
    }

    private void insertAllData() throws SQLException {
        insertSettings();
        insertUserProfile();
        insertMonthlyPlans();
        insertIncomeEntries();
        insertExpenseEntries();
        insertSavingsBuckets();
        insertSavingsEntries();
        insertGoals();
        insertGoalContributions();
        insertFamilyMembers();
        insertFamilyExpenseEntries();
        insertHabitRules();
        insertInvestments();
        insertInvestmentTransactions();
    }

    private void insertSettings() throws SQLException {
        String sql = "INSERT INTO app_settings (key, value, updated_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (var entry : listAppSettings().entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue());
                ps.setString(3, LocalDateTime.now().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertUserProfile() throws SQLException {
        UserProfile profile = getUserProfile();
        if (profile == null) {
            return;
        }
        String sql = """
                INSERT INTO user_profile (
                    singleton_key, id, first_name, last_name, email, age, profile_type, currency_code,
                    family_module_enabled, investments_module_enabled, achievements_module_enabled,
                    created_at, updated_at
                ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, profile.getId());
            ps.setString(2, profile.getFirstName());
            ps.setString(3, profile.getLastName());
            ps.setString(4, profile.getEmail());
            if (profile.getAge() == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, profile.getAge());
            }
            ps.setString(6, profile.getProfileType().name());
            ps.setString(7, profile.getCurrencyCode());
            ps.setInt(8, DbUtils.toIntBoolean(profile.isFamilyModuleEnabled()));
            ps.setInt(9, DbUtils.toIntBoolean(profile.isInvestmentsModuleEnabled()));
            ps.setInt(10, DbUtils.toIntBoolean(profile.isAchievementsModuleEnabled()));
            ps.setString(11, profile.getCreatedAt().toString());
            ps.setString(12, profile.getUpdatedAt().toString());
            ps.executeUpdate();
        }
    }

    private void insertMonthlyPlans() throws SQLException {
        String sql = """
                INSERT INTO monthly_plans (
                    month, id, fixed_costs_budget, food_budget, transport_budget, family_budget,
                    discretionary_budget, savings_percent, goals_percent, safety_buffer_amount,
                    notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (MonthlyPlan plan : listMonthlyPlans()) {
                ps.setString(1, plan.getMonth().toString());
                ps.setString(2, plan.getId());
                ps.setString(3, plan.getFixedCostsBudget().toPlainString());
                ps.setString(4, plan.getFoodBudget().toPlainString());
                ps.setString(5, plan.getTransportBudget().toPlainString());
                ps.setString(6, plan.getFamilyBudget().toPlainString());
                ps.setString(7, plan.getDiscretionaryBudget().toPlainString());
                ps.setString(8, plan.getSavingsPercent().toPlainString());
                ps.setString(9, plan.getGoalsPercent().toPlainString());
                ps.setString(10, plan.getSafetyBufferAmount().toPlainString());
                DbUtils.setNullableString(ps, 11, plan.getNotes());
                ps.setString(12, plan.getCreatedAt().toString());
                ps.setString(13, plan.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertIncomeEntries() throws SQLException {
        String sql = """
                INSERT INTO income_entries (
                    id, month, received_date, source_name, income_type, amount, recurring, received,
                    notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (IncomeEntry entry : listAllIncomeEntries()) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getMonth().toString());
                ps.setString(3, entry.getReceivedDate().toString());
                ps.setString(4, entry.getSourceName());
                ps.setString(5, entry.getIncomeType().name());
                ps.setString(6, entry.getAmount().toPlainString());
                ps.setInt(7, DbUtils.toIntBoolean(entry.isRecurring()));
                ps.setInt(8, DbUtils.toIntBoolean(entry.isReceived()));
                DbUtils.setNullableString(ps, 9, entry.getNotes());
                ps.setString(10, entry.getCreatedAt().toString());
                ps.setString(11, entry.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertExpenseEntries() throws SQLException {
        String sql = """
                INSERT INTO expense_entries (
                    id, month, expense_date, amount, category, subcategory, note, payment_method, tag,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (ExpenseEntry entry : listAllExpenseEntries()) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getMonth().toString());
                ps.setString(3, entry.getExpenseDate().toString());
                ps.setString(4, entry.getAmount().toPlainString());
                ps.setString(5, entry.getCategory().name());
                DbUtils.setNullableString(ps, 6, entry.getSubcategory());
                DbUtils.setNullableString(ps, 7, entry.getNote());
                ps.setString(8, entry.getPaymentMethod().name());
                DbUtils.setNullableString(ps, 9, entry.getTag());
                ps.setString(10, entry.getCreatedAt().toString());
                ps.setString(11, entry.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertSavingsBuckets() throws SQLException {
        String sql = """
                INSERT INTO savings_buckets (
                    id, name, current_amount, target_amount, active, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SavingsBucket bucket : listSavingsBuckets()) {
                ps.setString(1, bucket.getId());
                ps.setString(2, bucket.getName());
                ps.setString(3, bucket.getCurrentAmount().toPlainString());
                DbUtils.setNullableBigDecimal(ps, 4, bucket.getTargetAmount());
                ps.setInt(5, DbUtils.toIntBoolean(bucket.isActive()));
                DbUtils.setNullableString(ps, 6, bucket.getNotes());
                ps.setString(7, bucket.getCreatedAt().toString());
                ps.setString(8, bucket.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertSavingsEntries() throws SQLException {
        String sql = """
                INSERT INTO savings_entries (
                    id, bucket_id, month, entry_date, amount, entry_type, note, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SavingsEntry entry : listAllSavingsEntries()) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getBucketId());
                ps.setString(3, entry.getMonth().toString());
                ps.setString(4, entry.getEntryDate().toString());
                ps.setString(5, entry.getAmount().toPlainString());
                ps.setString(6, entry.getEntryType().name());
                DbUtils.setNullableString(ps, 7, entry.getNote());
                ps.setString(8, entry.getCreatedAt().toString());
                ps.setString(9, entry.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertGoals() throws SQLException {
        String sql = """
                INSERT INTO goals (
                    id, name, goal_type, target_amount, current_amount, target_date, priority, active,
                    notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Goal goal : listGoals()) {
                ps.setString(1, goal.getId());
                ps.setString(2, goal.getName());
                ps.setString(3, goal.getGoalType().name());
                ps.setString(4, goal.getTargetAmount().toPlainString());
                ps.setString(5, goal.getCurrentAmount().toPlainString());
                DbUtils.setNullableLocalDate(ps, 6, goal.getTargetDate());
                ps.setInt(7, goal.getPriority());
                ps.setInt(8, DbUtils.toIntBoolean(goal.isActive()));
                DbUtils.setNullableString(ps, 9, goal.getNotes());
                ps.setString(10, goal.getCreatedAt().toString());
                ps.setString(11, goal.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertGoalContributions() throws SQLException {
        String sql = """
                INSERT INTO goal_contributions (
                    id, goal_id, month, contribution_date, amount, type, note, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (GoalContribution entry : listAllGoalContributions()) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getGoalId());
                ps.setString(3, entry.getMonth().toString());
                ps.setString(4, entry.getContributionDate().toString());
                ps.setString(5, entry.getAmount().toPlainString());
                ps.setString(6, entry.getType().name());
                DbUtils.setNullableString(ps, 7, entry.getNote());
                ps.setString(8, entry.getCreatedAt().toString());
                ps.setString(9, entry.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertFamilyMembers() throws SQLException {
        String sql = """
                INSERT INTO family_members (
                    id, name, relationship_type, weekly_allowance, monthly_medical_budget,
                    monthly_support_budget, active, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (FamilyMember member : listFamilyMembers()) {
                ps.setString(1, member.getId());
                ps.setString(2, member.getName());
                ps.setString(3, member.getRelationshipType().name());
                ps.setString(4, member.getWeeklyAllowance().toPlainString());
                ps.setString(5, member.getMonthlyMedicalBudget().toPlainString());
                ps.setString(6, member.getMonthlySupportBudget().toPlainString());
                ps.setInt(7, DbUtils.toIntBoolean(member.isActive()));
                DbUtils.setNullableString(ps, 8, member.getNotes());
                ps.setString(9, member.getCreatedAt().toString());
                ps.setString(10, member.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertFamilyExpenseEntries() throws SQLException {
        String sql = """
                INSERT INTO family_expense_entries (
                    id, family_member_id, month, expense_date, amount, expense_type, note,
                    related_expense_entry_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (FamilyExpenseEntry entry : listAllFamilyExpenseEntries()) {
                ps.setString(1, entry.getId());
                ps.setString(2, entry.getFamilyMemberId());
                ps.setString(3, entry.getMonth().toString());
                ps.setString(4, entry.getExpenseDate().toString());
                ps.setString(5, entry.getAmount().toPlainString());
                ps.setString(6, entry.getExpenseType().name());
                DbUtils.setNullableString(ps, 7, entry.getNote());
                DbUtils.setNullableString(ps, 8, entry.getRelatedExpenseEntryId());
                ps.setString(9, entry.getCreatedAt().toString());
                ps.setString(10, entry.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertHabitRules() throws SQLException {
        String sql = """
                INSERT INTO habit_rules (
                    id, tag, display_name, linked_category, monthly_limit, warning_threshold_percent,
                    active, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (HabitRule rule : listHabitRules()) {
                ps.setString(1, rule.getId());
                ps.setString(2, rule.getTag());
                ps.setString(3, rule.getDisplayName());
                DbUtils.setNullableString(ps, 4, rule.getLinkedCategory() == null ? null : rule.getLinkedCategory().name());
                ps.setString(5, rule.getMonthlyLimit().toPlainString());
                ps.setString(6, rule.getWarningThresholdPercent().toPlainString());
                ps.setInt(7, DbUtils.toIntBoolean(rule.isActive()));
                DbUtils.setNullableString(ps, 8, rule.getNotes());
                ps.setString(9, rule.getCreatedAt().toString());
                ps.setString(10, rule.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertInvestments() throws SQLException {
        String sql = """
                INSERT INTO investments (
                    id, name, type, kind, status, target_amount, current_invested_amount,
                    current_estimated_value, expected_profit_amount, expected_profit_percent,
                    start_date, expected_return_date, priority, active, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Investment investment : listInvestments()) {
                ps.setString(1, investment.getId());
                ps.setString(2, investment.getName());
                ps.setString(3, investment.getType().name());
                ps.setString(4, investment.getKind().name());
                ps.setString(5, investment.getStatus().name());
                DbUtils.setNullableBigDecimal(ps, 6, investment.getTargetAmount());
                ps.setString(7, investment.getCurrentInvestedAmount().toPlainString());
                ps.setString(8, investment.getCurrentEstimatedValue().toPlainString());
                DbUtils.setNullableBigDecimal(ps, 9, investment.getExpectedProfitAmount());
                DbUtils.setNullableBigDecimal(ps, 10, investment.getExpectedProfitPercent());
                ps.setString(11, investment.getStartDate().toString());
                DbUtils.setNullableLocalDate(ps, 12, investment.getExpectedReturnDate());
                ps.setInt(13, investment.getPriority());
                ps.setInt(14, DbUtils.toIntBoolean(investment.isActive()));
                DbUtils.setNullableString(ps, 15, investment.getNotes());
                ps.setString(16, investment.getCreatedAt().toString());
                ps.setString(17, investment.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertInvestmentTransactions() throws SQLException {
        String sql = """
                INSERT INTO investment_transactions (
                    id, investment_id, month, transaction_date, amount, type, note, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (InvestmentTransaction tx : listAllInvestmentTransactions()) {
                ps.setString(1, tx.getId());
                ps.setString(2, tx.getInvestmentId());
                ps.setString(3, tx.getMonth().toString());
                ps.setString(4, tx.getTransactionDate().toString());
                ps.setString(5, tx.getAmount().toPlainString());
                ps.setString(6, tx.getType().name());
                DbUtils.setNullableString(ps, 7, tx.getNote());
                ps.setString(8, tx.getCreatedAt().toString());
                ps.setString(9, tx.getUpdatedAt().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
