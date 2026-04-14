package com.bitetogether.user.dto.conversation.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class DirectConversationResponse {
  private String conversationId;

  @JsonAlias("id")
  private String id;

  public String resolveConversationId() {
    return conversationId != null ? conversationId : id;
  }
}
