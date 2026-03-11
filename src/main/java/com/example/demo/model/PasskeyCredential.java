package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 儲存 WebAuthn / Passkey 憑證資料
 * - credentialId: Base64URL 編碼的憑證識別碼
 * - publicKeyCose: COSE 格式公鑰（byte array -> base64）
 * - signCount: 防重放計數器
 * - aaguid: 驗證器 AAGUID（設備類型識別）
 */
@Entity
@Table(name = "passkey_credentials", indexes = {
    @Index(name = "idx_passkey_credential_id", columnList = "credential_id", unique = true),
    @Index(name = "idx_passkey_user_id", columnList = "user_id")
})
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Base64URL 編碼的憑證 ID（來自 WebAuthn） */
    @Column(name = "credential_id", nullable = false, unique = true, length = 512)
    private String credentialId;

    /** COSE 編碼公鑰，以 Base64 儲存 */
    @Column(name = "public_key_cose", nullable = false, columnDefinition = "TEXT")
    private String publicKeyCose;

    /** 符合 WebAuthn 規範的簽署計數器（防重放） */
    @Column(name = "sign_count", nullable = false)
    private long signCount = 0L;

    /** 驗證器 AAGUID（可選，識別裝置品牌） */
    @Column(name = "aaguid", length = 36)
    private String aaguid;

    /** 使用者自定義名稱，例如「我的 iPhone」 */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /** User Handle（Base64URL），與 discoverable credential 對應 */
    @Column(name = "user_handle", length = 128)
    private String userHandle;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getPublicKeyCose() { return publicKeyCose; }
    public void setPublicKeyCose(String publicKeyCose) { this.publicKeyCose = publicKeyCose; }

    public long getSignCount() { return signCount; }
    public void setSignCount(long signCount) { this.signCount = signCount; }

    public String getAaguid() { return aaguid; }
    public void setAaguid(String aaguid) { this.aaguid = aaguid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getUserHandle() { return userHandle; }
    public void setUserHandle(String userHandle) { this.userHandle = userHandle; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
