package com.bitetogether.user.repository;

import com.bitetogether.user.model.RefreshToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
  @Modifying
  int deleteByExpiresAtBefore(LocalDateTime now);
}
