package com.bitetogether.user.convert;

import com.bitetogether.user.dto.friendrequest.response.FriendRequestResponse;
import com.bitetogether.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FriendRequestMapper {
  FriendRequestResponse toFriendRequestResponse(User friend);
}
