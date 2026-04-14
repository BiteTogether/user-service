package com.bitetogether.user.service;

import java.util.List;
import java.util.Map;

public interface ConversationService {
  String getDirectConversationId(Long otherUserId);

  Map<Long, String> getDirectConversationIds(List<Long> otherUserIds);
}
