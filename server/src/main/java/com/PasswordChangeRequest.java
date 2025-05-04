package com;

public class PasswordChangeRequest {
    private String username;
    private String newPassword;

    public PasswordChangeRequest(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }

    public String getUsername() { return username; }
    public String getNewPassword() { return newPassword; }

    public void setUsername(String username) { this.username = username; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
