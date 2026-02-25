package com.budgetpilot.service;

import com.budgetpilot.core.AppContext;
import com.budgetpilot.model.MonthlyPlan;
import com.budgetpilot.store.BudgetStore;
import com.budgetpilot.store.DbStore;
import com.budgetpilot.store.FullDataStore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.budgetpilot.util.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class BackupService {
    private static final DateTimeFormatter FILE_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppContext appContext;
    private final ObjectMapper mapper;

    public BackupService(AppContext appContext) {
        this.appContext = ValidationUtils.requireNonNull(appContext, "appContext");
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Path exportBackup() {
        FullDataStore store = requireFullDataStore();
        PersistenceStatus status = appContext.getPersistenceStatus();
        Path backupsPath = status == null || status.getBackupsPath() == null
                ? Path.of(System.getProperty("user.home"))
                : status.getBackupsPath();

        Path target = backupsPath.resolve("budgetpilot-backup-" + FILE_STAMP_FORMAT.format(LocalDateTime.now()) + ".json");
        BackupSnapshot snapshot = createSnapshot(store);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), snapshot);
            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export backup", ex);
        }
    }

    public void importBackup(Path backupFile) {
        ValidationUtils.requireNonNull(backupFile, "backupFile");
        if (!Files.isRegularFile(backupFile)) {
            throw new IllegalArgumentException("Backup file does not exist: " + backupFile);
        }
        FullDataStore store = requireFullDataStore();

        BackupSnapshot snapshot;
        try {
            snapshot = mapper.readValue(backupFile.toFile(), BackupSnapshot.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read backup file", ex);
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Backup file is empty or invalid.");
        }

        try {
            applySnapshot(store, snapshot);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to import backup data.", ex);
        }
        appContext.reloadCurrentUserFromStore();

        String selectedMonthValue = store.getAppSetting("selected_month");
        if (selectedMonthValue != null && !selectedMonthValue.isBlank()) {
            try {
                appContext.setSelectedMonth(YearMonth.parse(selectedMonthValue));
            } catch (Exception ignored) {
            }
        }
    }

    private BackupSnapshot createSnapshot(FullDataStore store) {
        BackupSnapshot snapshot = new BackupSnapshot();
        snapshot.setAppVersion("0.1.0");
        snapshot.setSchemaVersion(resolveSchemaVersion());
        snapshot.setExportedAt(LocalDateTime.now());

        BudgetStore budgetStore = appContext.getStore();
        snapshot.setUserProfile(budgetStore.getUserProfile());
        snapshot.setAppSettings(store.listAppSettings());
        snapshot.setMonthlyPlans(store.listMonthlyPlans());
        snapshot.setIncomeEntries(store.listAllIncomeEntries());
        snapshot.setExpenseEntries(store.listAllExpenseEntries());
        snapshot.setSavingsBuckets(budgetStore.listSavingsBuckets());
        snapshot.setSavingsEntries(store.listAllSavingsEntries());
        snapshot.setGoals(budgetStore.listGoals());
        snapshot.setGoalContributions(store.listAllGoalContributions());
        snapshot.setFamilyMembers(budgetStore.listFamilyMembers());
        snapshot.setFamilyExpenseEntries(store.listAllFamilyExpenseEntries());
        snapshot.setHabitRules(budgetStore.listHabitRules());
        snapshot.setInvestments(budgetStore.listInvestments());
        snapshot.setInvestmentTransactions(store.listAllInvestmentTransactions());

        return snapshot;
    }

    private void applySnapshot(FullDataStore store, BackupSnapshot snapshot) {
        BudgetStore budgetStore = appContext.getStore();
        Runnable apply = () -> {
            budgetStore.clearAll();

            Map<String, String> settings = snapshot.getAppSettings();
            if (settings != null) {
                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    store.saveAppSetting(entry.getKey(), entry.getValue());
                }
            }

            if (snapshot.getUserProfile() != null) {
                budgetStore.saveUserProfile(snapshot.getUserProfile());
            }

            saveAllMonthlyPlans(budgetStore, snapshot.getMonthlyPlans());
            saveAllIncome(budgetStore, snapshot.getIncomeEntries());
            saveAllExpenses(budgetStore, snapshot.getExpenseEntries());
            saveAllSavingsBuckets(budgetStore, snapshot.getSavingsBuckets());
            saveAllSavingsEntries(budgetStore, snapshot.getSavingsEntries());
            saveAllGoals(budgetStore, snapshot.getGoals());
            saveAllGoalContributions(budgetStore, snapshot.getGoalContributions());
            saveAllFamilyMembers(budgetStore, snapshot.getFamilyMembers());
            saveAllFamilyExpenses(budgetStore, snapshot.getFamilyExpenseEntries());
            saveAllHabitRules(budgetStore, snapshot.getHabitRules());
            saveAllInvestments(budgetStore, snapshot.getInvestments());
            saveAllInvestmentTransactions(budgetStore, snapshot.getInvestmentTransactions());
        };

        if (budgetStore instanceof DbStore dbStore) {
            dbStore.runBulkUpdate(apply);
        } else {
            apply.run();
        }
    }

    private void saveAllMonthlyPlans(BudgetStore store, List<MonthlyPlan> plans) {
        if (plans == null) {
            return;
        }
        for (MonthlyPlan plan : plans) {
            if (plan != null) {
                store.saveMonthlyPlan(plan);
            }
        }
    }

    private void saveAllIncome(BudgetStore store, List<com.budgetpilot.model.IncomeEntry> entries) {
        if (entries == null) {
            return;
        }
        for (com.budgetpilot.model.IncomeEntry entry : entries) {
            if (entry != null) {
                store.saveIncomeEntry(entry);
            }
        }
    }

    private void saveAllExpenses(BudgetStore store, List<com.budgetpilot.model.ExpenseEntry> entries) {
        if (entries == null) {
            return;
        }
        for (com.budgetpilot.model.ExpenseEntry entry : entries) {
            if (entry != null) {
                store.saveExpenseEntry(entry);
            }
        }
    }

    private void saveAllSavingsBuckets(BudgetStore store, List<com.budgetpilot.model.SavingsBucket> buckets) {
        if (buckets == null) {
            return;
        }
        for (com.budgetpilot.model.SavingsBucket bucket : buckets) {
            if (bucket != null) {
                store.saveSavingsBucket(bucket);
            }
        }
    }

    private void saveAllSavingsEntries(BudgetStore store, List<com.budgetpilot.model.SavingsEntry> entries) {
        if (entries == null) {
            return;
        }
        for (com.budgetpilot.model.SavingsEntry entry : entries) {
            if (entry != null) {
                store.saveSavingsEntry(entry);
            }
        }
    }

    private void saveAllGoals(BudgetStore store, List<com.budgetpilot.model.Goal> goals) {
        if (goals == null) {
            return;
        }
        for (com.budgetpilot.model.Goal goal : goals) {
            if (goal != null) {
                store.saveGoal(goal);
            }
        }
    }

    private void saveAllGoalContributions(BudgetStore store, List<com.budgetpilot.model.GoalContribution> entries) {
        if (entries == null) {
            return;
        }
        for (com.budgetpilot.model.GoalContribution entry : entries) {
            if (entry != null) {
                store.saveGoalContribution(entry);
            }
        }
    }

    private void saveAllFamilyMembers(BudgetStore store, List<com.budgetpilot.model.FamilyMember> members) {
        if (members == null) {
            return;
        }
        for (com.budgetpilot.model.FamilyMember member : members) {
            if (member != null) {
                store.saveFamilyMember(member);
            }
        }
    }

    private void saveAllFamilyExpenses(BudgetStore store, List<com.budgetpilot.model.FamilyExpenseEntry> entries) {
        if (entries == null) {
            return;
        }
        for (com.budgetpilot.model.FamilyExpenseEntry entry : entries) {
            if (entry != null) {
                store.saveFamilyExpenseEntry(entry);
            }
        }
    }

    private void saveAllHabitRules(BudgetStore store, List<com.budgetpilot.model.HabitRule> rules) {
        if (rules == null) {
            return;
        }
        for (com.budgetpilot.model.HabitRule rule : rules) {
            if (rule != null) {
                store.saveHabitRule(rule);
            }
        }
    }

    private void saveAllInvestments(BudgetStore store, List<com.budgetpilot.model.Investment> investments) {
        if (investments == null) {
            return;
        }
        for (com.budgetpilot.model.Investment investment : investments) {
            if (investment != null) {
                store.saveInvestment(investment);
            }
        }
    }

    private void saveAllInvestmentTransactions(BudgetStore store, List<com.budgetpilot.model.InvestmentTransaction> txs) {
        if (txs == null) {
            return;
        }
        for (com.budgetpilot.model.InvestmentTransaction tx : txs) {
            if (tx != null) {
                store.saveInvestmentTransaction(tx);
            }
        }
    }

    private int resolveSchemaVersion() {
        BudgetStore store = appContext.getStore();
        if (store instanceof DbStore dbStore) {
            return dbStore.getSchemaVersion();
        }
        return 0;
    }

    private FullDataStore requireFullDataStore() {
        BudgetStore store = ValidationUtils.requireNonNull(appContext.getStore(), "store");
        if (store instanceof FullDataStore fullDataStore) {
            return fullDataStore;
        }
        throw new IllegalStateException("Current store does not support full-data backup/export");
    }
}
