package com.ledgerline.util;

import com.ledgerline.enums.Role;
import com.ledgerline.exception.UnauthorizedException;
import com.ledgerline.security.CustomUserDetailsService.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("User is not authenticated");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static Role getCurrentUserRole() {
        String roleStr = getCurrentUser().getRole();
        return Role.valueOf(roleStr);
    }
}

