package com.miracle.smart_ecommerce_security.domain.auth.service.impl;

import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom OIDC user service for Google login.
 *
 * Google uses OpenID Connect (OIDC), so Spring Security routes through OidcUserService —
 * NOT DefaultOAuth2UserService. Extending OidcUserService ensures our loadUser() is
 * actually called and that we return an OidcUser (not DefaultOAuth2User), preserving
 * the OIDC ID token and all claims.
 *
 * The saved User entity is embedded into the OidcUser attributes under key "appUser"
 * so OAuth2AuthenticationSuccessHandler can read it without a second DB query.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends OidcUserService {

    public static final String APP_USER_ATTRIBUTE = "appUser";

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider   = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerId = oidcUser.getAttribute("sub");
        String email      = oidcUser.getAttribute("email");
        String firstName  = oidcUser.getAttribute("given_name");
        String lastName   = oidcUser.getAttribute("family_name");

        log.info("OAUTH2_LOGIN — Provider: {} — ProviderId: {} — Email: {}", provider, providerId, email);

        // 1. Lookup by provider + providerId, then fallback to email
        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthProviderId(provider, providerId);
        if (existingUser.isEmpty() && email != null) {
            existingUser = userRepository.findByEmailAddress(email);
        }

        User appUser;
        if (existingUser.isPresent()) {
            appUser = existingUser.get();
            if (appUser.getOauthProvider() == null) {
                appUser.setOauthProvider(provider);
                appUser.setOauthProviderId(providerId);
                appUser = userRepository.saveAndFlush(appUser);
                log.info("OAUTH2_LINKED — UserId: {} — Provider: {}", appUser.getId(), provider);
            }
            log.info("OAUTH2_LOGIN_SUCCESS — UserId: {} — Role: {}", appUser.getId(), appUser.getRole());
        } else {
            User newUser = User.builder()
                    .emailAddress(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .passwordHash(null)
                    .isActive(true)
                    .role("CUSTOMER")
                    .oauthProvider(provider)
                    .oauthProviderId(providerId)
                    .build();
            appUser = userRepository.saveAndFlush(newUser);
            log.info("OAUTH2_USER_CREATED — UserId: {} — Email: {} — Provider: {}",
                    appUser.getId(), email, provider);
        }

        // 2. Embed appUser into attributes — return a proper OidcUser so the ID token is preserved
        Map<String, Object> attributes = new HashMap<>(oidcUser.getAttributes());
        attributes.put(APP_USER_ATTRIBUTE, appUser);

        return new DefaultOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "sub"
        ) {
            @Override
            public Map<String, Object> getAttributes() {
                return attributes;
            }
        };
    }
}
