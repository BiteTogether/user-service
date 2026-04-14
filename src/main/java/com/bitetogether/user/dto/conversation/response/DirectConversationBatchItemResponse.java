package com.bitetogether.user.dto.conversation.response;

import lombok.Data;

@Data
public class DirectConversationBatchItemResponse {
  private Long userId;
  private String conversationId;
}
