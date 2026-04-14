package com.bitetogether.user.client;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.user.configuration.openfeign.FeignClientConfig;
import com.bitetogether.user.dto.conversation.request.DirectConversationBatchRequest;
import com.bitetogether.user.dto.conversation.response.DirectConversationBatchResponse;
import com.bitetogether.user.dto.conversation.response.DirectConversationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "chat-service",
    url = "${feign.chat-service.url}",
    configuration = FeignClientConfig.class)
public interface ConversationClient {

  @GetMapping("/api/v1/conversations/direct")
  ApiResponseDTO<DirectConversationResponse> getDirectConversation(
      @RequestParam("otherUserId") Long otherUserId);

  @PostMapping("/api/v1/conversations/direct/batch")
  ApiResponseDTO<DirectConversationBatchResponse> getDirectConversationsBatch(
      @RequestBody DirectConversationBatchRequest request);
}
