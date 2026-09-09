package com.example.storefront.security;

/** Coarse-grained authority. Stored as a string in {@code app_user_role.role}. */
public enum Role {
    CUSTOMER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
