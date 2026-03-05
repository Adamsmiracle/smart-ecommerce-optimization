package com.miracle.smart_ecommerce_security.domain.auth.service.impl;

import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security UserDetailsService that loads users from the database.
 * Used by AuthenticationManager for credential-based login and by OAuth2 for user merging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> {
                    log.warn("USER_NOT_FOUND — Email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        if (user.getIsActive() != null && !user.getIsActive()) {
            log.warn("USER_INACTIVE — Email: {} — UserId: {}", email, user.getId());
            throw new UsernameNotFoundException("User account is deactivated: " + email);
        }

        String role = user.getRole() != null ? user.getRole() : "CUSTOMER";
        String grantedRole = role.toUpperCase().startsWith("ROLE_")
                ? role.toUpperCase()
                : "ROLE_" + role.toUpperCase();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmailAddress())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(List.of(new SimpleGrantedAuthority(grantedRole)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(Boolean.FALSE.equals(user.getIsActive()))
                .build();
    }
}

