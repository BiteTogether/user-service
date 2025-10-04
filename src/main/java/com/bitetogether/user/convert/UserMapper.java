package com.bitetogether.user.convert;

import com.bitetogether.user.dto.friend.response.FriendResponse;
import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.dto.user.request.UpdateUserRequest;
import com.bitetogether.user.dto.user.response.UserResponse;
import com.bitetogether.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
  User toEntity(CreateUserRequest createUserRequest);

  UserResponse toUserResponse(User user);

  void updateUserFromRequest(UpdateUserRequest updateUserRequest, @MappingTarget User user);

  FriendResponse toFriendResponse(User user);
}
