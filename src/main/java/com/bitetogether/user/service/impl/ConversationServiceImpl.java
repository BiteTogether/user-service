package com.bitetogether.user.service.impl;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.user.client.ConversationClient;
import com.bitetogether.user.dto.conversation.request.DirectConversationBatchRequest;
import com.bitetogether.user.dto.conversation.response.DirectConversationBatchItemResponse;
import com.bitetogether.user.dto.conversation.response.DirectConversationBatchResponse;
import com.bitetogether.user.dto.conversation.response.DirectConversationResponse;
import com.bitetogether.user.service.ConversationService;
import feign.FeignException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
  private final ConversationClient conversationClient;

  @Override
  public String getDirectConversationId(Long otherUserId) {
    if (otherUserId == null) {
      return null;
    }

    try {
      ApiResponseDTO<DirectConversationResponse> response =
          conversationClient.getDirectConversation(otherUserId);
      DirectConversationResponse payload = response == null ? null : response.getData();
      return payload == null ? null : payload.resolveConversationId();
    } catch (FeignException ex) {
      log.warn(
          "Failed to fetch direct conversation for user {}. Status: {}",
          otherUserId,
          ex.status());
      return null;
    } catch (Exception ex) {
      log.warn("Failed to parse direct conversation response for user {}", otherUserId, ex);
      return null;
    }
  }

  @Override
  public Map<Long, String> getDirectConversationIds(List<Long> otherUserIds) {
    if (otherUserIds == null || otherUserIds.isEmpty()) {
      return Collections.emptyMap();
    }

    try {
      DirectConversationBatchRequest request =
          DirectConversationBatchRequest.builder().userIds(otherUserIds).build();
      ApiResponseDTO<DirectConversationBatchResponse> response =
          conversationClient.getDirectConversationsBatch(request);

      DirectConversationBatchResponse payload = response == null ? null : response.getData();
      if (payload == null || payload.getItems() == null || payload.getItems().isEmpty()) {
        return Collections.emptyMap();
      }

      Map<Long, String> conversationIds = new LinkedHashMap<>();
      for (DirectConversationBatchItemResponse item : payload.getItems()) {
        if (item != null && item.getUserId() != null) {
          conversationIds.put(item.getUserId(), item.getConversationId());
        }
      }
      return conversationIds;
    } catch (FeignException ex) {
      log.warn(
          "Failed to fetch direct conversation batch for {} users. Status: {}",
          otherUserIds.size(),
          ex.status());
      return Collections.emptyMap();
    } catch (Exception ex) {
      log.warn("Failed to parse direct conversation batch response", ex);
      return Collections.emptyMap();
    }
  }
}
