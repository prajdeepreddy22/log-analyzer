package com.loganalyzer.repository;

import com.loganalyzer.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    Optional<ApiToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<ApiToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ApiToken> findByIdAndUserId(Long id, Long userId);
}
