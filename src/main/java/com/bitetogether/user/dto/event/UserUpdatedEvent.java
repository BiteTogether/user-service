package com.bitetogether.user.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent {
  private Long userId;
  private String username;
  private String fullName;
  private String phoneNumber;
  private String avatar;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime eventTimestamp;

  private Long version; // For optimistic locking
}

