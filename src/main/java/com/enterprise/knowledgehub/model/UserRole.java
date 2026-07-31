package com.enterprise.knowledgehub.model;

/**
 * Defines the roles available in KnowledgeHub.
 * - ADMIN: Can manage all documents (view, download, delete).
 * - EMPLOYEE: Can view and search all documents, upload, download, and delete their own.
 */
public enum UserRole {
    ADMIN,
    EMPLOYEE
}
