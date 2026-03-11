package com.example.demo.dto;

/**
 * Passkey 認證完成請求
 * rawResponse: 前端 JSON.stringify(credential) 的字串
 */
public class PasskeyAssertionFinishRequest {

    /** 可為 null，支援 discoverable credential（免輸入帳號） */
    private String username;
    private String rawResponse;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
}
