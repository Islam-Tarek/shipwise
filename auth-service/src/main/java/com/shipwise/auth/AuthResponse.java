package com.shipwise.auth;

public record AuthResponse(String token, UserAccount user) {
}