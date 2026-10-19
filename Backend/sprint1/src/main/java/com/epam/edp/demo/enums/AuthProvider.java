package com.epam.edp.demo.enums;

/**
 * Enum representing the authentication provider for a user account.
 * LOCAL = standard email/password login
 * GOOGLE = Google OAuth2 social login
 * GITHUB = GitHub OAuth2 social login
 * FACEBOOK = Facebook OAuth2 social login (code-ready, requires HTTPS for production)
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    GITHUB,
    FACEBOOK
}
