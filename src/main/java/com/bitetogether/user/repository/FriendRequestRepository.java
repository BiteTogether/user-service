package com.bitetogether.user.repository;

import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

  boolean existsBySenderAndReceiver(User sender, User receiver);

  Optional<FriendRequest> findById(Long id);

  Page<FriendRequest> findBySenderId(Long senderId, Pageable pageable);

  Page<FriendRequest> findByReceiverId(Long receiverId, Pageable pageable);

  Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

  @Modifying
  @Query("DELETE FROM FriendRequest fr WHERE fr.sender.id = :userId OR fr.receiver.id = :userId")
  void deleteAllByUserId(@Param("userId") Long userId);
}
