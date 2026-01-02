package com.aiagent.security.oauth2;

import com.aiagent.entity.User.AuthProvider;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.name())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.GITHUB.name())) {
            return new GitHubOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        }
    }
}
