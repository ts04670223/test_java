package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.PasskeyCredential;
import com.example.demo.model.User;
import com.example.demo.service.JwtService;
import com.example.demo.service.WebAuthnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebAuthn / Passkeys API
 *
 * 註冊流程:
 *   POST /api/passkeys/registration/start   → 取得 PublicKeyCredentialCreationOptions
 *   POST /api/passkeys/registration/finish  → 提交驗證器回應，完成登錄
 *
 * 認證流程:
 *   POST /api/passkeys/assertion/start      → 取得 PublicKeyCredentialRequestOptions
 *   POST /api/passkeys/assertion/finish     → 提交驗證器回應，取得 JWT
 *
 * 管理:
 *   GET    /api/passkeys                    → 查看我的 Passkeys
 *   DELETE /api/passkeys/{credentialId}     → 刪除 Passkey
 */
@RestController
@RequestMapping("/api/passkeys")
@Tag(name = "Passkeys", description = "WebAuthn 生物辨識無密碼驗證 API")
public class WebAuthnController {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnController.class);

    // Session key 常數
    private static final String SESSION_REGISTRATION_OPTIONS = "webauthn_registration_options";
    private static final String SESSION_ASSERTION_REQUEST    = "webauthn_assertion_request";
    private static final String SESSION_ASSERTION_USERNAME   = "webauthn_assertion_username";

    private final WebAuthnService webAuthnService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public WebAuthnController(WebAuthnService webAuthnService,
                               JwtService jwtService,
                               ObjectMapper objectMapper) {
        this.webAuthnService = webAuthnService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 註冊 (Registration)
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/registration/start")
    @Operation(summary = "開始 Passkey 註冊", description = "產生 WebAuthn 挑戰，回傳給前端驅動驗證器")
    public ResponseEntity<ApiResponse<Object>> startRegistration(
            @RequestBody PasskeyRegistrationStartRequest request,
            HttpSession session) {
        try {
            PublicKeyCredentialCreationOptions options =
                    webAuthnService.startRegistration(request.getUsername(), request.getDisplayName());

            // 暫存 challenge 到伺服器 session（防 CSRF）
            session.setAttribute(SESSION_REGISTRATION_OPTIONS, options.toJson());
            session.setAttribute("webauthn_reg_username", request.getUsername());
            session.setAttribute("webauthn_reg_displayname", request.getDisplayName());

            // 回傳 JSON 給前端（WebAuthn API 需要特定格式）
            Object optionsJson = objectMapper.readValue(options.toJson(), Object.class);
            return ResponseEntity.ok(ApiResponse.success("請完成生物辨識驗證", optionsJson));
        } catch (IllegalArgumentException e) {
            // 不洩露使用者是否存在（防使用者列舉攻擊）
            log.debug("startRegistration 業務錯誤: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("無法啟動 Passkey 註冊，請確認您已登入"));
        } catch (Exception e) {
            log.error("startRegistration 失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("無法啟動 Passkey 註冊"));
        }
    }

    @PostMapping("/registration/finish")
    @Operation(summary = "完成 Passkey 註冊", description = "驗證驗證器回應並儲存 Passkey")
    public ResponseEntity<ApiResponse<PasskeyCredentialResponse>> finishRegistration(
            @RequestBody PasskeyRegistrationFinishRequest request,
            HttpSession session) {
        try {
            String optionsJson = (String) session.getAttribute(SESSION_REGISTRATION_OPTIONS);
            String username = (String) session.getAttribute("webauthn_reg_username");
            String displayName = (String) session.getAttribute("webauthn_reg_displayname");

            if (optionsJson == null || username == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("找不到待完成的 Passkey 註冊，請重新開始"));
            }

            // 清除 session 中的暫存資料（用過即棄）
            session.removeAttribute(SESSION_REGISTRATION_OPTIONS);
            session.removeAttribute("webauthn_reg_username");
            session.removeAttribute("webauthn_reg_displayname");

            PublicKeyCredentialCreationOptions options =
                    PublicKeyCredentialCreationOptions.fromJson(optionsJson);

            @SuppressWarnings("unchecked")
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential =
                    PublicKeyCredential.parseRegistrationResponseJson(request.getRawResponse());

            PasskeyCredential saved = webAuthnService.finishRegistration(
                    username, options, credential, displayName);

            PasskeyCredentialResponse resp = toResponse(saved);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Passkey 登錄成功！下次可直接使用生物辨識登入", resp));

        } catch (RegistrationFailedException e) {
            log.warn("Passkey 註冊驗證失敗: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Passkey 驗證失敗：" + e.getMessage()));
        } catch (Exception e) {
            log.error("finishRegistration 失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("完成 Passkey 註冊時發生錯誤"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 認證 (Authentication / Assertion)
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/assertion/start")
    @Operation(summary = "開始 Passkey 登入", description = "產生認證挑戰，支援 discoverable credential（免輸入帳號）")
    public ResponseEntity<ApiResponse<Object>> startAssertion(
            @RequestParam(required = false) String username,
            HttpSession session) {
        try {
            AssertionRequest assertionRequest = webAuthnService.startAssertion(username);

            session.setAttribute(SESSION_ASSERTION_REQUEST, assertionRequest.toJson());
            session.setAttribute(SESSION_ASSERTION_USERNAME, username);

            Object requestJson = objectMapper.readValue(assertionRequest.toJson(), Object.class);
            return ResponseEntity.ok(ApiResponse.success("請完成生物辨識驗證", requestJson));
        } catch (Exception e) {
            log.error("startAssertion 失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("無法啟動 Passkey 登入"));
        }
    }

    @PostMapping("/assertion/finish")
    @Operation(summary = "完成 Passkey 登入", description = "驗證簽名並回傳 JWT Token")
    public ResponseEntity<ApiResponse<LoginResponse>> finishAssertion(
            @RequestBody PasskeyAssertionFinishRequest request,
            HttpSession session) {
        try {
            String assertionJson = (String) session.getAttribute(SESSION_ASSERTION_REQUEST);

            if (assertionJson == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("找不到待完成的 Passkey 認證，請重新開始"));
            }

            session.removeAttribute(SESSION_ASSERTION_REQUEST);
            session.removeAttribute(SESSION_ASSERTION_USERNAME);

            AssertionRequest assertionRequest = AssertionRequest.fromJson(assertionJson);

            @SuppressWarnings("unchecked")
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential =
                    PublicKeyCredential.parseAssertionResponseJson(request.getRawResponse());

            User user = webAuthnService.finishAssertion(assertionRequest, credential);

            // 登入成功後輪換 Session ID，防止 Session 固定攻擊
            session.invalidate();

            String jwt = jwtService.generateToken(user);

            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            userResponse.setFirstName(user.getFirstName());
            userResponse.setLastName(user.getLastName());
            userResponse.setRole(user.getRole().name());

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(jwt);
            loginResponse.setUser(userResponse);

            return ResponseEntity.ok(ApiResponse.success("Passkey 登入成功", loginResponse));

        } catch (AssertionFailedException e) {
            // 不回傳內部錯誤細節（防資訊洩露）
            log.warn("Passkey 認證失敗: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Passkey 驗證失敗，請再試一次"));
        } catch (Exception e) {
            log.error("finishAssertion 失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("完成 Passkey 登入時發生錯誤"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 憑證管理
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping
    @Operation(summary = "查看我的 Passkeys", description = "取得目前使用者已登錄的所有 Passkey 列表")
    public ResponseEntity<ApiResponse<List<PasskeyCredentialResponse>>> listMyPasskeys(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("請先登入"));
        }
        List<PasskeyCredentialResponse> list = webAuthnService.listCredentials(user.getUsername())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("取得 Passkey 列表成功", list));
    }

    @DeleteMapping("/{credentialId}")
    @Operation(summary = "刪除 Passkey", description = "刪除指定的 Passkey 憑證")
    public ResponseEntity<ApiResponse<Void>> deletePasskey(
            @PathVariable String credentialId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("請先登入"));
        }
        try {
            webAuthnService.deleteCredential(user.getUsername(), credentialId);
            return ResponseEntity.ok(ApiResponse.success("Passkey 已刪除", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────────────

    private PasskeyCredentialResponse toResponse(PasskeyCredential cred) {
        PasskeyCredentialResponse r = new PasskeyCredentialResponse();
        r.setId(cred.getId());
        r.setCredentialId(cred.getCredentialId());
        r.setDisplayName(cred.getDisplayName());
        // aaguid 不回傳給前端：屬裝置型號識別碼，可被第三方用於跨站裝置指紋追蹤
        r.setCreatedAt(cred.getCreatedAt());
        r.setLastUsedAt(cred.getLastUsedAt());
        return r;
    }
}
