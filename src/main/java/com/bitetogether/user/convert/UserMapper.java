package com.bitetogether.user.convert;

import com.bitetogether.user.dto.user.request.CreateUserRequest;
import com.bitetogether.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  public User toEntity(CreateUserRequest createUserRequest);
}
