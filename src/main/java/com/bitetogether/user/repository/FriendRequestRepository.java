package com.bitetogether.user.repository;

import com.bitetogether.user.model.FriendRequest;
import com.bitetogether.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsBySenderAndReceiver(User sender, User receiver);

    Optional<FriendRequest> findById(Long id);
}
