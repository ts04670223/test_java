package com.example.demo.dto;

/**
 * Passkey 註冊啟動請求
 */
public class PasskeyRegistrationStartRequest {

    private String username;
    private String displayName;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
