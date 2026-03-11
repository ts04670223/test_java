package com.example.demo.service;

import com.example.demo.model.PasskeyCredential;
import com.example.demo.model.User;
import com.example.demo.repository.PasskeyCredentialRepository;
import com.example.demo.repository.UserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 實作 Yubico WebAuthn SDK 的 CredentialRepository 介面，
 * 讓 RelyingParty 能夠從資料庫查找憑證。
 */
@Component
public class WebAuthnCredentialRepository implements CredentialRepository {

    private final PasskeyCredentialRepository passkeyRepo;
    private final UserRepository userRepository;

    public WebAuthnCredentialRepository(PasskeyCredentialRepository passkeyRepo,
                                         UserRepository userRepository) {
        this.passkeyRepo = passkeyRepo;
        this.userRepository = userRepository;
    }

    /**
     * 根據使用者名稱查詢其所有已登錄的 Passkey 描述符（用於認證請求）
     */
    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return passkeyRepo.findByUserUsername(username).stream()
                .map(cred -> PublicKeyCredentialDescriptor.builder()
                        .id(base64UrlDecode(cred.getCredentialId()))
                        .build())
                .collect(Collectors.toSet());
    }

    /**
     * 根據使用者名稱取得 userHandle（Base64URL ByteArray）
     */
    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    // 以 user ID 作為 user handle（8 bytes big-endian）
                    byte[] idBytes = longToBytes(user.getId());
                    return new ByteArray(idBytes);
                });
    }

    /**
     * 根據 userHandle 反查使用者名稱
     */
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        long userId = bytesToLong(userHandle.getBytes());
        return userRepository.findById(userId)
                .map(User::getUsername);
    }

    /**
     * 根據憑證 ID 與使用者 ID 查詢已登錄的公鑰憑證
     */
    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        String credIdBase64 = base64UrlEncode(credentialId);
        return passkeyRepo.findByCredentialId(credIdBase64)
                .map(cred -> RegisteredCredential.builder()
                        .credentialId(credentialId)
                        .userHandle(userHandle)
                        .publicKeyCose(base64Decode(cred.getPublicKeyCose()))
                        .signatureCount(cred.getSignCount())
                        .build());
    }

    /**
     * 根據憑證 ID 查詢所有符合的已登錄憑證（不需 userHandle）
     */
    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        String credIdBase64 = base64UrlEncode(credentialId);
        return passkeyRepo.findByCredentialId(credIdBase64)
                .map(cred -> {
                    byte[] userHandleBytes = longToBytes(cred.getUser().getId());
                    return RegisteredCredential.builder()
                            .credentialId(credentialId)
                            .userHandle(new ByteArray(userHandleBytes))
                            .publicKeyCose(base64Decode(cred.getPublicKeyCose()))
                            .signatureCount(cred.getSignCount())
                            .build();
                })
                .map(Set::of)
                .orElse(Set.of());
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    private ByteArray base64UrlDecode(String s) {
        return new ByteArray(Base64.getUrlDecoder().decode(s));
    }

    private String base64UrlEncode(ByteArray b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b.getBytes());
    }

    private ByteArray base64Decode(String s) {
        return new ByteArray(Base64.getDecoder().decode(s));
    }

    private byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        return value;
    }
}
