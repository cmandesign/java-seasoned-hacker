package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * SpEL-accessible bean that checks whether the authenticated user
 * owns the requested resource or has ADMIN role.
 */
@Component("accountOwnership")
public class AccountOwnershipCheck {

    private final UserRepository userRepository;

    public AccountOwnershipCheck(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isOwner(Authentication auth, Long userId) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true; // admins can access any account
        }
        return userRepository.findByUsername(auth.getName())
                .map(user -> user.getId().equals(userId))
                .orElse(false);
    }
}
