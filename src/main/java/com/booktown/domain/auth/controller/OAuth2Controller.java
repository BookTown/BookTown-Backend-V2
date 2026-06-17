package com.booktown.domain.auth.controller;

import com.booktown.domain.auth.service.AuthService;
import com.booktown.domain.auth.service.OAuth2LoginService;
import com.booktown.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth2")
@Tag(name = "OAuth2", description = "Google, Kakao, Naver 소셜 로그인 API")
public class OAuth2Controller {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final OAuth2LoginService oAuth2LoginService;

    @GetMapping("/{provider}/login")
    @Operation(summary = "소셜 로그인 시작", description = "`provider`는 `google`, `kakao`, `naver` 중 하나입니다. Provider 인증 페이지로 302 redirect합니다.")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(oAuth2LoginService.buildAuthorizationUrl(provider)))
                .build();
    }

    @GetMapping("/{provider}/callback")
    @Operation(summary = "소셜 로그인 callback", description = "Provider 인가 코드를 처리하고 프론트 성공/실패 페이지로 redirect합니다.")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        if (error != null && !error.isBlank()) {
            return redirect(oAuth2LoginService.buildFailureRedirectUri(error));
        }

        try {
            AuthService.TokenPair tokenPair = oAuth2LoginService.loginWithAuthorizationCode(provider, code, state);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(oAuth2LoginService.buildSuccessRedirectUri(tokenPair)))
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(tokenPair).toString())
                    .build();
        } catch (CustomException e) {
            return redirect(oAuth2LoginService.buildFailureRedirectUri(e.getErrorCode().name()));
        }
    }

    private ResponseEntity<Void> redirect(String uri) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(uri))
                .build();
    }

    private ResponseCookie refreshCookie(AuthService.TokenPair tokenPair) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, tokenPair.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(tokenPair.refreshTokenExpirationMs()))
                .build();
    }
}
