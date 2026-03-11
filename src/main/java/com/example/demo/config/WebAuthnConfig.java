package com.example.demo.config;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.service.WebAuthnCredentialRepository;

/**
 * WebAuthn / Passkeys 設定
 *
 * rpId   = 網域名稱，必須與前端 origin 相符
 * rpName = 顯示在瀏覽器 Passkey 提示中的應用名稱
 */
@Configuration
public class WebAuthnConfig {

    @Value("${webauthn.rp.id:localhost}")
    private String rpId;

    @Value("${webauthn.rp.name:Spring Boot Demo}")
    private String rpName;

    @Bean
    public RelyingParty relyingParty(WebAuthnCredentialRepository credentialRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepository)
                .allowOriginPort(true)    // 允許帶埠號的 origin（開發環境使用）
                .allowOriginSubdomain(false)
                .build();
    }
}
