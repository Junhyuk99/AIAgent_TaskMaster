package com.aiagent.security.oauth2;

import com.aiagent.entity.User;
import com.aiagent.entity.User.AuthProvider;
import com.aiagent.repository.UserRepository;
import com.aiagent.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("OAuth2 user processing failed", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(userInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                // Link existing local account with OAuth
                user.setAuthProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
                user.setProviderId(userInfo.getId());
                if (userInfo.getImageUrl() != null) {
                    user.setProfileImageUrl(userInfo.getImageUrl());
                }
                user = userRepository.save(user);
                log.info("Linked OAuth2 provider {} to existing user: {}", registrationId, user.getEmail());
            } else if (!user.getAuthProvider().name().equalsIgnoreCase(registrationId)) {
                throw new OAuth2AuthenticationException(
                    "You're signed up with " + user.getAuthProvider() + " account. " +
                    "Please use your " + user.getAuthProvider() + " account to login."
                );
            } else {
                // Update user info
                user = updateExistingUser(user, userInfo);
            }
        } else {
            user = registerNewUser(registrationId, userInfo);
        }

        return UserPrincipal.create(user, oauth2User.getAttributes());
    }

    private User registerNewUser(String registrationId, OAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .password(UUID.randomUUID().toString()) // Random password for OAuth users
                .profileImageUrl(userInfo.getImageUrl())
                .authProvider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(userInfo.getId())
                .isActive(true)
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("Registered new OAuth2 user: {} via {}", user.getEmail(), registrationId);
        return user;
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setName(userInfo.getName());
        if (userInfo.getImageUrl() != null) {
            user.setProfileImageUrl(userInfo.getImageUrl());
        }
        return userRepository.save(user);
    }
}
