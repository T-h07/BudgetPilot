package com.budgetpilot.ui.components;

import com.budgetpilot.util.MoneyUtils;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class MoneyField extends TextField {
    private final String fieldName;

    public MoneyField(String fieldName, String promptText) {
        this.fieldName = fieldName;
        setPromptText(promptText);
        getStyleClass().addAll("text-input", "money-input");
    }

    public BigDecimal parseRequiredPositive() {
        return MoneyUtils.parse(getText(), fieldName, false);
    }

    public BigDecimal parseNonNegativeOrZero() {
        return MoneyUtils.parseNonNegativeOrZero(getText(), fieldName);
    }

    public void setMoneyValue(BigDecimal value) {
        setText(MoneyUtils.normalize(value).toPlainString());
    }
}
