package com.bitetogether.user.repository;

import com.bitetogether.user.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByFirebaseUid(String firebaseUid);

  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsByUsername(String username);

  Optional<User> findByUsername(String username);

  @Query("SELECT f FROM User u JOIN u.friends f WHERE u.id = :userId")
  Page<User> getFriendsByUserId(@Param("userId") Long userId, Pageable pageable);

  Optional<User> findByPhoneNumber(String phoneNumber);
}
