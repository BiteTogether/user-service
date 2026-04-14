package com.bitetogether.user.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGetByIdResponse {
  private Long id;
  private String username;
  private String fullName;
  private String phoneNumber;
  private String avatar;
  private String conversationId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UserGetByIdItem friendItem;
}
