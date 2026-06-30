package com.minesweeper.auth.dto;

import com.minesweeper.auth.model.User;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Dopo login o registrazione: token, refresh token e profilo (nessuna password in risposta). */
public class AuthResponseBody {

    @JsonProperty("token")
    private String token;
    @JsonProperty("refreshToken")
    private String refreshToken;
    @JsonProperty("user")
    private User user;

    public AuthResponseBody() {}

    public AuthResponseBody(String token, String refreshToken, User user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
