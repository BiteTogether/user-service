package com.bitetogether.user.repository;

import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

  boolean existsBySenderAndReceiver(User sender, User receiver);

  Optional<FriendRequest> findById(Long id);

  Page<FriendRequest> findBySenderId(Long senderId, Pageable pageable);

  Page<FriendRequest> findByReceiverId(Long receiverId, Pageable pageable);
}
