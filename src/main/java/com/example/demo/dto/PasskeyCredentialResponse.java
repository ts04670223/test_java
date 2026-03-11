package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * 已登錄 Passkey 憑證的摘要資訊（回傳給前端管理頁面）
 */
public class PasskeyCredentialResponse {

    private Long id;
    private String credentialId;
    private String displayName;
    private String aaguid;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAaguid() { return aaguid; }
    public void setAaguid(String aaguid) { this.aaguid = aaguid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
