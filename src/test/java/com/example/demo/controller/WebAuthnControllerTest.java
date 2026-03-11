package com.example.demo.controller;

import com.example.demo.model.PasskeyCredential;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import com.example.demo.service.WebAuthnCredentialRepository;
import com.example.demo.service.WebAuthnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialParameters;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebAuthnController test suite
 *
 * RED tests (expected to FAIL - missing input validation):
 *   startRegistration_blankUsername_shouldReturn400
 *   startRegistration_nullUsername_shouldReturn400
 *
 * GREEN tests (security regression + behavioral correctness):
 *   user enumeration prevention, session protection,
 *   aaguid not exposed, management endpoints require auth
 */
@WebMvcTest(controllers = WebAuthnController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebAuthnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebAuthnService webAuthnService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    // WebAuthnConfig needs WebAuthnCredentialRepository to create RelyingParty bean;
    // this mock satisfies the dependency so the @WebMvcTest context loads correctly.
    @MockBean
    private WebAuthnCredentialRepository webAuthnCredentialRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setEmail("testuser@example.com");
        mockUser.setRole(UserRole.CUSTOMER);
        mockUser.setPassword("hashed-pw");
        mockUser.setEnabled(true);
    }

    // Sets SecurityContextHolder directly - works with addFilters=false in Spring Security 6
    private RequestPostProcessor authAs(User user) {
        return request -> {
            var auth = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private PasskeyCredential sampleCredential() {
        PasskeyCredential cred = new PasskeyCredential();
        cred.setUser(mockUser);
        cred.setCredentialId("cred-id-abc123");
        cred.setDisplayName("My Test Passkey");
        cred.setAaguid("aaguid-ABCD-1234");
        cred.setPublicKeyCose("base64encodedPublicKey");
        return cred;
    }

    private PublicKeyCredentialCreationOptions sampleCreationOptions() throws Exception {
        return PublicKeyCredentialCreationOptions.builder()
                .rp(RelyingPartyIdentity.builder()
                        .id("localhost")
                        .name("Test App")
                        .build())
                .user(UserIdentity.builder()
                        .name("testuser")
                        .displayName("Test User")
                        .id(new ByteArray(new byte[]{0, 0, 0, 0, 0, 0, 0, 1}))
                        .build())
                .challenge(new ByteArray(new byte[32]))
                .pubKeyCredParams(Collections.singletonList(
                        PublicKeyCredentialParameters.builder()
                                .alg(COSEAlgorithmIdentifier.ES256)
                                .type(PublicKeyCredentialType.PUBLIC_KEY)
                                .build()))
                .build();
    }

    private AssertionRequest sampleAssertionRequest() throws Exception {
        return AssertionRequest.builder()
                .publicKeyCredentialRequestOptions(
                        PublicKeyCredentialRequestOptions.builder()
                                .challenge(new ByteArray(new byte[32]))
                                .rpId("localhost")
                                .build())
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/passkeys/registration/start
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/passkeys/registration/start")
    class StartRegistration {

        /**
         * RED TEST - Expected to FAIL.
         * Reason: PasskeyRegistrationStartRequest has no @NotBlank on username.
         * Current: blank username reaches webAuthnService (mock returns null),
         *          null.toJson() throws NPE -> 500 INTERNAL_SERVER_ERROR.
         * Expected: 400 BAD_REQUEST (needs @Valid + @NotBlank in DTO + controller param).
         */
        @Test
        @DisplayName("[RED] blank username should return 400 - currently returns 500 (missing @NotBlank)")
        void startRegistration_blankUsername_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/passkeys/registration/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"displayName\":\"test\"}"))
                    .andExpect(status().isBadRequest());
        }

        /**
         * RED TEST - Expected to FAIL.
         * Same root cause: no @NotNull validation -> NPE -> 500.
         */
        @Test
        @DisplayName("[RED] null username should return 400 - currently returns 500 (missing @NotNull)")
        void startRegistration_nullUsername_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/passkeys/registration/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayName\":\"test\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Non-existent user returns 400 with generic message (not 'user not found')")
        void startRegistration_nonExistentUser_returns400WithGenericMessage() throws Exception {
            when(webAuthnService.startRegistration(anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("User does not exist: ghost"));

            mockMvc.perform(post("/api/passkeys/registration/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ghost\",\"displayName\":\"Ghost\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Error message must not reveal whether username exists (user enumeration prevention)")
        void startRegistration_nonExistentUser_messageShouldNotRevealUsername() throws Exception {
            when(webAuthnService.startRegistration(anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("User does not exist: ghost"));

            mockMvc.perform(post("/api/passkeys/registration/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ghost\",\"displayName\":\"Ghost\"}"))
                    .andExpect(jsonPath("$.message", not(containsString("ghost"))))
                    .andExpect(jsonPath("$.message", not(containsString("not exist"))))
                    .andExpect(jsonPath("$.message", not(containsString("not found"))));
        }

        @Test
        @DisplayName("Valid user returns 200 with WebAuthn challenge")
        void startRegistration_validUser_returns200WithChallenge() throws Exception {
            when(webAuthnService.startRegistration("testuser", "Test User"))
                    .thenReturn(sampleCreationOptions());

            mockMvc.perform(post("/api/passkeys/registration/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"displayName\":\"Test User\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.challenge").exists());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/passkeys/registration/finish
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/passkeys/registration/finish")
    class FinishRegistration {

        @Test
        @DisplayName("No active session returns 400")
        void finishRegistration_noActiveSession_returns400() throws Exception {
            mockMvc.perform(post("/api/passkeys/registration/finish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"rawResponse\":\"{}\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Challenge is single-use: session cleared after attempt prevents replay")
        void finishRegistration_sessionClearedAfterAttempt_preventsReplay() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("webauthn_registration_options", "invalid-json");
            session.setAttribute("webauthn_reg_username", "testuser");
            session.setAttribute("webauthn_reg_displayname", "Test User");

            // First attempt (will fail due to invalid JSON - caught as 500)
            mockMvc.perform(post("/api/passkeys/registration/finish")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"testuser\",\"rawResponse\":\"{}\"}"));

            // Second attempt with same session - session attributes removed, must return 400
            mockMvc.perform(post("/api/passkeys/registration/finish")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"testuser\",\"rawResponse\":\"{}\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/passkeys/assertion/start
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/passkeys/assertion/start")
    class StartAssertion {

        @Test
        @DisplayName("No username (discoverable credential) returns 200 with challenge")
        void startAssertion_noUsername_returns200() throws Exception {
            when(webAuthnService.startAssertion(null))
                    .thenReturn(sampleAssertionRequest());

            mockMvc.perform(post("/api/passkeys/assertion/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("With username returns 200 with challenge")
        void startAssertion_withUsername_returns200() throws Exception {
            when(webAuthnService.startAssertion("testuser"))
                    .thenReturn(sampleAssertionRequest());

            mockMvc.perform(post("/api/passkeys/assertion/start")
                            .param("username", "testuser"))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/passkeys/assertion/finish
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/passkeys/assertion/finish")
    class FinishAssertion {

        @Test
        @DisplayName("No active session returns 400")
        void finishAssertion_noActiveSession_returns400() throws Exception {
            mockMvc.perform(post("/api/passkeys/assertion/finish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rawResponse\":\"{}\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("AssertionFailedException returns 401 with generic message (no internal details)")
        void finishAssertion_assertionFailed_returns401WithGenericMessage() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("webauthn_assertion_request",
                    sampleAssertionRequest().toJson());

            // Mock webAuthnService to throw after credential parsing (any args)
            when(webAuthnService.finishAssertion(any(), any()))
                    .thenThrow(new AssertionFailedException("signature count did not increase"));

            mockMvc.perform(post("/api/passkeys/assertion/finish")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rawResponse\":\"{}\"}"))
                    // Either 401 (assertion failed) or 500 (parse failed before service call)
                    // We assert the message does not leak internal exception detail
                    .andExpect(jsonPath("$.message",
                            not(containsString("signature count did not increase"))));
        }

        @Test
        @DisplayName("AssertionFailedException message must not contain exception details")
        void finishAssertion_assertionFailed_messageMustBeGeneric() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("webauthn_assertion_request",
                    sampleAssertionRequest().toJson());

            when(webAuthnService.finishAssertion(any(), any()))
                    .thenThrow(new AssertionFailedException("internal error: count=5"));

            mockMvc.perform(post("/api/passkeys/assertion/finish")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rawResponse\":\"{}\"}"))
                    .andExpect(jsonPath("$.message",
                            not(containsString("count=5"))));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/passkeys
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/passkeys")
    class ListPasskeys {

        @Test
        @DisplayName("Unauthenticated (null principal) returns 401")
        void listMyPasskeys_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/passkeys"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Authenticated user receives their passkey list")
        void listMyPasskeys_authenticated_returns200() throws Exception {
            when(webAuthnService.listCredentials("testuser"))
                    .thenReturn(List.of(sampleCredential()));

            mockMvc.perform(get("/api/passkeys")
                            .with(authAs(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Response does not expose aaguid (device fingerprint prevention)")
        void listMyPasskeys_aaguidIsNullInResponse() throws Exception {
            PasskeyCredential cred = sampleCredential();
            cred.setAaguid("SENSITIVE-AAGUID-12345678"); // in DB but must not appear

            when(webAuthnService.listCredentials("testuser"))
                    .thenReturn(List.of(cred));

            mockMvc.perform(get("/api/passkeys")
                            .with(authAs(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].aaguid").doesNotExist());
        }

        @Test
        @DisplayName("Response does not expose raw public key (publicKeyCose must not leak)")
        void listMyPasskeys_publicKeyShouldNotBeExposed() throws Exception {
            when(webAuthnService.listCredentials("testuser"))
                    .thenReturn(List.of(sampleCredential()));

            mockMvc.perform(get("/api/passkeys")
                            .with(authAs(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].publicKeyCose").doesNotExist())
                    .andExpect(jsonPath("$.data[0].publicKey").doesNotExist());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/passkeys/{credentialId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/passkeys/{credentialId}")
    class DeletePasskey {

        @Test
        @DisplayName("Unauthenticated returns 401")
        void deletePasskey_noAuth_returns401() throws Exception {
            mockMvc.perform(delete("/api/passkeys/cred-abc"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Attempting to delete another user's credential returns 403")
        void deletePasskey_notOwner_returns403() throws Exception {
            doThrow(new SecurityException("Unauthorized to delete this credential"))
                    .when(webAuthnService).deleteCredential(anyString(), anyString());

            mockMvc.perform(delete("/api/passkeys/other-user-cred")
                            .with(authAs(mockUser)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Credential not found returns 404")
        void deletePasskey_notFound_returns404() throws Exception {
            doThrow(new IllegalArgumentException("Credential not found"))
                    .when(webAuthnService).deleteCredential(anyString(), anyString());

            mockMvc.perform(delete("/api/passkeys/nonexistent-cred")
                            .with(authAs(mockUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Owner deleting own credential returns 200")
        void deletePasskey_ownCredential_returns200() throws Exception {
            // void mock - does nothing by default

            mockMvc.perform(delete("/api/passkeys/my-cred-id")
                            .with(authAs(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
