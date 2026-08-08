package com.enterprise.knowledgehub.model;

/**
 * Represents the predefined departments within the organization.
 * Each document uploaded to KnowledgeHub must belong to exactly one department.
 */
public enum Department {
    HR("Human Resources"),
    ENGINEERING("Engineering"),
    FINANCE("Finance"),
    LEGAL("Legal"),
    OPERATIONS("Operations");

    private final String displayName;

    Department(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
