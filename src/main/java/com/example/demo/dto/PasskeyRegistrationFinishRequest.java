package com.example.demo.dto;

/**
 * Passkey 註冊完成請求
 * rawResponse: 前端 JSON.stringify(credential) 的字串
 */
public class PasskeyRegistrationFinishRequest {

    private String username;
    private String displayName;
    private String rawResponse;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
}
