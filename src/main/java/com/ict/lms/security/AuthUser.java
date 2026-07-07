package com.ict.lms.security;

import com.ict.lms.model.Role;

/**
 * The logged-in user, rebuilt from the JWT on every request.
 * Injected into controllers with @AuthenticationPrincipal.
 */
public record AuthUser(Long id, String email, Role role, Integer grade, String name) {
}
