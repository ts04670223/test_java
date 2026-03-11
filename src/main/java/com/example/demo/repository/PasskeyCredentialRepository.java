package com.example.demo.repository;

import com.example.demo.model.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    List<PasskeyCredential> findByUserId(Long userId);

    List<PasskeyCredential> findByUserUsername(String username);

    boolean existsByCredentialId(String credentialId);

    void deleteByCredentialId(String credentialId);

    @Modifying
    @Query("UPDATE PasskeyCredential p SET p.signCount = :signCount, p.lastUsedAt = :lastUsedAt WHERE p.credentialId = :credentialId")
    int updateSignCount(@Param("credentialId") String credentialId,
                        @Param("signCount") long signCount,
                        @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
