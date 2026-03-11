package com.example.demo.service;

import com.example.demo.model.PasskeyCredential;
import com.example.demo.model.User;
import com.example.demo.repository.PasskeyCredentialRepository;
import com.example.demo.repository.UserRepository;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * WebAuthn / Passkeys 核心服務
 *
 * 流程:
 *   [註冊] startRegistration() -> finishRegistration()
 *   [登入] startAssertion()    -> finishAssertion()
 */
@Service
public class WebAuthnService {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnService.class);

    private final RelyingParty relyingParty;
    private final UserRepository userRepository;
    private final PasskeyCredentialRepository passkeyRepo;

    public WebAuthnService(RelyingParty relyingParty,
                           UserRepository userRepository,
                           PasskeyCredentialRepository passkeyRepo) {
        this.relyingParty = relyingParty;
        this.userRepository = userRepository;
        this.passkeyRepo = passkeyRepo;
    }

    // ── 註冊流程 ─────────────────────────────────────────────────────────────

    /**
     * 步驟1：產生註冊選項（challenge），回傳給前端
     */
    public PublicKeyCredentialCreationOptions startRegistration(String username, String displayName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在: " + username));

        byte[] userHandleBytes = longToBytes(user.getId());

        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(displayName != null ? displayName : username)
                .id(new ByteArray(userHandleBytes))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .residentKey(ResidentKeyRequirement.PREFERRED)
                        .build())
                .build();

        return relyingParty.startRegistration(options);
    }

    /**
     * 步驟2：驗證並儲存新的 Passkey 憑證
     */
    @Transactional
    public PasskeyCredential finishRegistration(String username,
                                                 PublicKeyCredentialCreationOptions requestOptions,
                                                 PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential,
                                                 String displayName)
            throws RegistrationFailedException {

        RegistrationResult result = relyingParty.finishRegistration(
                FinishRegistrationOptions.builder()
                        .request(requestOptions)
                        .response(credential)
                        .build());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在: " + username));

        PasskeyCredential passkeyCredential = new PasskeyCredential();
        passkeyCredential.setUser(user);
        passkeyCredential.setCredentialId(base64UrlEncode(result.getKeyId().getId()));
        passkeyCredential.setPublicKeyCose(base64Encode(result.getPublicKeyCose()));
        passkeyCredential.setSignCount(result.getSignatureCount());
        ByteArray aaguidBytes = result.getAaguid();
        passkeyCredential.setAaguid(aaguidBytes != null ? aaguidBytes.getHex() : null);
        passkeyCredential.setDisplayName(displayName != null ? displayName : "Passkey");
        passkeyCredential.setUserHandle(base64UrlEncode(new ByteArray(longToBytes(user.getId()))));

        log.info("使用者 {} 成功登錄 Passkey", username);
        log.debug("新增 Passkey credentialId={}", passkeyCredential.getCredentialId());
        return passkeyRepo.save(passkeyCredential);
    }

    // ── 認證流程 ─────────────────────────────────────────────────────────────

    /**
     * 步驟1：產生認證選項（challenge），回傳給前端
     * username 可為 null（支援 discoverable credential）
     */
    public AssertionRequest startAssertion(String username) {
        StartAssertionOptions.StartAssertionOptionsBuilder builder =
                StartAssertionOptions.builder()
                        .userVerification(UserVerificationRequirement.PREFERRED);

        if (username != null && !username.isBlank()) {
            builder.username(username);
        }

        return relyingParty.startAssertion(builder.build());
    }

    /**
     * 步驟2：驗證簽名並取得登入使用者
     */
    @Transactional
    public User finishAssertion(AssertionRequest assertionRequest,
                                 PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential)
            throws AssertionFailedException {

        AssertionResult result = relyingParty.finishAssertion(
                FinishAssertionOptions.builder()
                        .request(assertionRequest)
                        .response(credential)
                        .build());

        if (!result.isSuccess()) {
            throw new AssertionFailedException("Passkey 驗證失敗");
        }

        String credentialId = base64UrlEncode(result.getCredential().getCredentialId());
        passkeyRepo.updateSignCount(credentialId, result.getSignatureCount(), LocalDateTime.now());

        log.info("Passkey 驗證成功，使用者: {}", result.getUsername());
        log.debug("更新 signCount credentialId={}", credentialId);

        return userRepository.findByUsername(result.getUsername())
                .orElseThrow(() -> new IllegalStateException("驗證成功但找不到使用者: " + result.getUsername()));
    }

    // ── Passkey 管理 ─────────────────────────────────────────────────────────

    public List<PasskeyCredential> listCredentials(String username) {
        return passkeyRepo.findByUserUsername(username);
    }

    @Transactional
    public void deleteCredential(String username, String credentialId) {
        PasskeyCredential cred = passkeyRepo.findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("憑證不存在"));
        if (!cred.getUser().getUsername().equals(username)) {
            throw new SecurityException("無權刪除此憑證");
        }
        passkeyRepo.deleteByCredentialId(credentialId);
        log.info("使用者 {} 已刪除一筆 Passkey", username);
        log.debug("已刪除 credentialId={}", credentialId);
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    private String base64UrlEncode(ByteArray b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b.getBytes());
    }

    private String base64Encode(ByteArray b) {
        return Base64.getEncoder().encodeToString(b.getBytes());
    }

    private byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }
}
