package com.booktown.domain.auth.service;

import com.booktown.domain.auth.entity.AuthProvider;
import com.booktown.global.exception.CustomException;
import com.booktown.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2LoginService {

    private final OAuth2Properties properties;

    public String buildAuthorizationUrl(String providerValue) {
        AuthProvider provider = parseProvider(providerValue);
        OAuth2Properties.Provider providerProperties = properties.getProviders()
                .get(provider.name().toLowerCase(Locale.ROOT));
        if (providerProperties == null || isBlank(providerProperties.getClientId())) {
            throw new CustomException(ErrorCode.UNSUPPORTED_AUTH_PROVIDER);
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(providerProperties.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", providerProperties.getClientId())
                .queryParam("redirect_uri", providerProperties.getRedirectUri())
                .queryParam("state", UUID.randomUUID());

        if (!isBlank(providerProperties.getScope())) {
            builder.queryParam("scope", providerProperties.getScope());
        }

        return builder.build(true).toUriString();
    }

    private AuthProvider parseProvider(String providerValue) {
        try {
            AuthProvider provider = AuthProvider.from(providerValue);
            if (provider == AuthProvider.LOCAL) {
                throw new CustomException(ErrorCode.UNSUPPORTED_AUTH_PROVIDER);
            }
            return provider;
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.UNSUPPORTED_AUTH_PROVIDER);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
