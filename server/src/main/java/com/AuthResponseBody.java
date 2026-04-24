package com;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Dopo login o registrazione: token e profilo (nessuna password in risposta). */
public class AuthResponseBody {

    @JsonProperty("token")
    private String token;
    @JsonProperty("user")
    private User user;

    public AuthResponseBody() {}

    public AuthResponseBody(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
