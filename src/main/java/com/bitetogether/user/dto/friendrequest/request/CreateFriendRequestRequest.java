package com.bitetogether.user.dto.friendrequest.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFriendRequestRequest {

    @NotNull(message = "Receiver ID cannot be null")
    @Positive(message = "Receiver ID must be a positive number")
    private Long receiverId;

}
