package com.bitetogether.user.dto.user.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchResponse {
  Long id;
  String username;
  String fullName;
  String avatar;
}
