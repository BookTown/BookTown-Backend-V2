package com.booktown.domain.auth.controller;

import com.booktown.domain.auth.dto.AuthTokenResponse;
import com.booktown.domain.auth.dto.LoginRequest;
import com.booktown.domain.auth.dto.ReissueRequest;
import com.booktown.domain.auth.dto.SignupRequest;
import com.booktown.domain.auth.security.UserPrincipal;
import com.booktown.domain.auth.service.AuthService;
import com.booktown.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthService.TokenPair tokenPair = authService.signup(request);
        return tokenResponse(tokenPair);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.TokenPair tokenPair = authService.login(request);
        return tokenResponse(tokenPair);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> reissue(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieRefreshToken,
            @RequestBody(required = false) ReissueRequest request
    ) {
        String refreshToken = cookieRefreshToken != null ? cookieRefreshToken : (request == null ? null : request.refreshToken());
        AuthService.TokenPair tokenPair = authService.reissue(refreshToken);
        return tokenResponse(tokenPair);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<AuthTokenResponse>> tokenResponse(AuthService.TokenPair tokenPair) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokenPair).toString())
                .body(ApiResponse.success(tokenPair.response()));
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

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
