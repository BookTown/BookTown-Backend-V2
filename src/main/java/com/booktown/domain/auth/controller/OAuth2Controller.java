package com.booktown.domain.auth.controller;

import com.booktown.domain.auth.service.OAuth2LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth2")
public class OAuth2Controller {

    private final OAuth2LoginService oAuth2LoginService;

    @GetMapping("/{provider}/login")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(oAuth2LoginService.buildAuthorizationUrl(provider)))
                .build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .header(HttpHeaders.WARNING, provider + " OAuth2 callback exchange is not implemented yet")
                .build();
    }
}
