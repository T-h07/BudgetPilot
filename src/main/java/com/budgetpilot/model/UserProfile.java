package com.budgetpilot.model;

import com.budgetpilot.model.enums.UserProfileType;
import com.budgetpilot.util.ValidationUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public class UserProfile {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private UserProfileType profileType;
    private String currencyCode;
    private boolean familyModuleEnabled;
    private boolean investmentsModuleEnabled;
    private boolean achievementsModuleEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserProfile() {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID().toString();
        this.firstName = "Budget";
        this.lastName = "User";
        this.email = "user@budgetpilot.local";
        this.age = 25;
        this.profileType = UserProfileType.PERSONAL_USE;
        this.currencyCode = "EUR";
        this.familyModuleEnabled = false;
        this.investmentsModuleEnabled = false;
        this.achievementsModuleEnabled = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UserProfile(String firstName, String lastName, String email, UserProfileType profileType) {
        this();
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setProfileType(profileType);
    }

    public UserProfile(UserProfile other) {
        ValidationUtils.requireNonNull(other, "other");
        this.id = other.id;
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.email = other.email;
        this.age = other.age;
        this.profileType = other.profileType;
        this.currencyCode = other.currencyCode;
        this.familyModuleEnabled = other.familyModuleEnabled;
        this.investmentsModuleEnabled = other.investmentsModuleEnabled;
        this.achievementsModuleEnabled = other.achievementsModuleEnabled;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public UserProfile copy() {
        return new UserProfile(this);
    }

    public String getDisplayName() {
        return (firstName + " " + lastName).trim();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
        touch();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = ValidationUtils.requireNonBlank(firstName, "firstName");
        touch();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = ValidationUtils.requireNonBlank(lastName, "lastName");
        touch();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = ValidationUtils.requireNonBlank(email, "email");
        touch();
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        if (age != null && age < 0) {
            throw new IllegalArgumentException("age must be non-negative");
        }
        this.age = age;
        touch();
    }

    public UserProfileType getProfileType() {
        return profileType;
    }

    public void setProfileType(UserProfileType profileType) {
        this.profileType = ValidationUtils.requireNonNull(profileType, "profileType");
        touch();
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = ValidationUtils.requireNonBlank(currencyCode, "currencyCode")
                .toUpperCase(Locale.ROOT);
        touch();
    }

    public boolean isFamilyModuleEnabled() {
        return familyModuleEnabled;
    }

    public void setFamilyModuleEnabled(boolean familyModuleEnabled) {
        this.familyModuleEnabled = familyModuleEnabled;
        touch();
    }

    public boolean isInvestmentsModuleEnabled() {
        return investmentsModuleEnabled;
    }

    public void setInvestmentsModuleEnabled(boolean investmentsModuleEnabled) {
        this.investmentsModuleEnabled = investmentsModuleEnabled;
        touch();
    }

    public boolean isAchievementsModuleEnabled() {
        return achievementsModuleEnabled;
    }

    public void setAchievementsModuleEnabled(boolean achievementsModuleEnabled) {
        this.achievementsModuleEnabled = achievementsModuleEnabled;
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = ValidationUtils.requireNonNull(createdAt, "createdAt");
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = ValidationUtils.requireNonNull(updatedAt, "updatedAt");
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id='" + id + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", email='" + email + '\'' +
                ", profileType=" + profileType +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
