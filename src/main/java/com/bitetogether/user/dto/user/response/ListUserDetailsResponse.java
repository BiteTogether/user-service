package com.bitetogether.user.dto.user.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListUserDetailsResponse {
  private List<UserDetailsResponse> users;
}
