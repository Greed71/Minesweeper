package com;

public class PasswordChangeRequest {
    private String currentPassword;  // Aggiungi la password attuale
    private String newPassword;

    // Costruttore con tutti i campi
    public PasswordChangeRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    // Getter e Setter per la password attuale
    public String getCurrentPassword() { 
        return currentPassword; 
    }
    
    public void setCurrentPassword(String currentPassword) { 
        this.currentPassword = currentPassword; 
    }

    // Getter e Setter per la nuova password
    public String getNewPassword() { 
        return newPassword; 
    }

    public void setNewPassword(String newPassword) { 
        this.newPassword = newPassword; 
    }
}
