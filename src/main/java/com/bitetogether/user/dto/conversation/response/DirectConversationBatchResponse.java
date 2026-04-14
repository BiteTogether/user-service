package com.bitetogether.user.dto.conversation.response;

import java.util.List;
import lombok.Data;

@Data
public class DirectConversationBatchResponse {
  private List<DirectConversationBatchItemResponse> items;
}
