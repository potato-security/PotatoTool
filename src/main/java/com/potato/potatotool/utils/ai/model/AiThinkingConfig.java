package com.potato.potatotool.utils.ai.model;

public class AiThinkingConfig {
    private final boolean enabled;
    private final int budgetTokens;

    public AiThinkingConfig(boolean enabled, int budgetTokens) {
        this.enabled = enabled;
        this.budgetTokens = budgetTokens;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getBudgetTokens() {
        return budgetTokens;
    }
}
