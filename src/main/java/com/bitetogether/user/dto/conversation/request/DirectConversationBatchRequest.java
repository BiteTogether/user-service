package com.bitetogether.user.dto.conversation.request;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DirectConversationBatchRequest {
  private List<Long> userIds;
}
