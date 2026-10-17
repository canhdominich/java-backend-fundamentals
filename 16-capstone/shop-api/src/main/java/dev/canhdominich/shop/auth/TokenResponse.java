package dev.canhdominich.shop.auth;

public record TokenResponse(String token, String tokenType, long expiresInSeconds) {
}
