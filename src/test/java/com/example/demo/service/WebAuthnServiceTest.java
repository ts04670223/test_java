package com.example.demo.service;

import com.example.demo.model.PasskeyCredential;
import com.example.demo.model.User;
import com.example.demo.repository.PasskeyCredentialRepository;
import com.example.demo.repository.UserRepository;
import com.yubico.webauthn.RelyingParty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * WebAuthnService 單元測試
 *
 * 覆蓋範圍：
 *   - 使用者不存在時的例外處理
 *   - 憑證擁有者驗證（跨帳號操作保護）
 *   - 憑證列表查詢
 */
@ExtendWith(MockitoExtension.class)
class WebAuthnServiceTest {

    @Mock
    private RelyingParty relyingParty;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasskeyCredentialRepository passkeyRepo;

    @InjectMocks
    private WebAuthnService service;

    // ─── 共用資料 ───────────────────────────────────────────────────────────

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setPassword("pw");
        alice.setEnabled(true);

        bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setPassword("pw");
        bob.setEnabled(true);
    }

    private PasskeyCredential credentialOwnedBy(User owner) {
        PasskeyCredential c = new PasskeyCredential();
        c.setUser(owner);
        c.setCredentialId("cred-xyz-001");
        c.setDisplayName("Alice's MacBook");
        c.setPublicKeyCose("dummyPublicKey");
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // startRegistration
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startRegistration()")
    class StartRegistration {

        @Test
        @DisplayName("使用者不存在 → 拋出 IllegalArgumentException（不能暗示哪個帳號存在）")
        void startRegistration_unknownUser_throwsIllegalArgumentException() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startRegistration("ghost", "Ghost"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // listCredentials
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listCredentials()")
    class ListCredentials {

        @Test
        @DisplayName("使用者無 Passkey → 回傳空列表")
        void listCredentials_userWithNoPasskeys_returnsEmptyList() {
            when(passkeyRepo.findByUserUsername("alice")).thenReturn(List.of());

            List<PasskeyCredential> result = service.listCredentials("alice");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("使用者有多筆 Passkey → 全部回傳")
        void listCredentials_userWithPasskeys_returnsAll() {
            PasskeyCredential c1 = credentialOwnedBy(alice);
            PasskeyCredential c2 = credentialOwnedBy(alice);
            c2.setCredentialId("cred-xyz-002");
            c2.setDisplayName("Alice's iPhone");

            when(passkeyRepo.findByUserUsername("alice")).thenReturn(List.of(c1, c2));

            List<PasskeyCredential> result = service.listCredentials("alice");

            assertThat(result).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // deleteCredential
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteCredential()")
    class DeleteCredential {

        @Test
        @DisplayName("憑證不存在 → 拋出 IllegalArgumentException")
        void deleteCredential_credentialNotFound_throwsIllegalArgumentException() {
            when(passkeyRepo.findByCredentialId("nonexistent-cred"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCredential("alice", "nonexistent-cred"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("憑證不存在");
        }

        @Test
        @DisplayName("刪除他人的憑證 → 拋出 SecurityException（越權操作保護）")
        void deleteCredential_notOwner_throwsSecurityException() {
            // 憑證屬於 alice，bob 嘗試刪除
            PasskeyCredential aliceCred = credentialOwnedBy(alice);
            when(passkeyRepo.findByCredentialId("cred-xyz-001"))
                    .thenReturn(Optional.of(aliceCred));

            assertThatThrownBy(() -> service.deleteCredential("bob", "cred-xyz-001"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("無權刪除");
        }

        @Test
        @DisplayName("擁有者刪除自己的憑證 → 不拋出例外")
        void deleteCredential_byOwner_succeeds() {
            PasskeyCredential aliceCred = credentialOwnedBy(alice);
            when(passkeyRepo.findByCredentialId("cred-xyz-001"))
                    .thenReturn(Optional.of(aliceCred));

            // 不拋出例外即視為成功
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> service.deleteCredential("alice", "cred-xyz-001"));
        }
    }
}
