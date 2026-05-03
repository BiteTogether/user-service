package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.user.client.ConversationClient;
import com.bitetogether.user.dto.conversation.response.DirectConversationBatchItemResponse;
import com.bitetogether.user.dto.conversation.response.DirectConversationBatchResponse;
import com.bitetogether.user.dto.conversation.response.DirectConversationResponse;
import feign.FeignException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {
  @Mock private ConversationClient conversationClient;
  @InjectMocks private ConversationServiceImpl conversationService;

  @Test
  void getDirectConversationId_WithNullUserId_ReturnsNull() {
    assertNull(conversationService.getDirectConversationId(null));
  }

  @Test
  void getDirectConversationId_WithValidUser_ReturnsId() {
    DirectConversationResponse r = new DirectConversationResponse();

    r.setConversationId("conv-1");
    when(conversationClient.getDirectConversation(1L))
        .thenReturn(ApiResponseDTO.<DirectConversationResponse>builder().data(r).build());
    assertEquals("conv-1", conversationService.getDirectConversationId(1L));
  }

  @Test
  void getDirectConversationId_FallsBackToId() {

    DirectConversationResponse r = new DirectConversationResponse();
    r.setId("id-1");
    when(conversationClient.getDirectConversation(1L))
        .thenReturn(ApiResponseDTO.<DirectConversationResponse>builder().data(r).build());
    assertEquals("id-1", conversationService.getDirectConversationId(1L));
  }

  @Test
  void getDirectConversationId_NullResponse_ReturnsNull() {
    when(conversationClient.getDirectConversation(1L)).thenReturn(null);
    assertNull(conversationService.getDirectConversationId(1L));
  }

  @Test
  void getDirectConversationId_NullData_ReturnsNull() {
    when(conversationClient.getDirectConversation(1L))
        .thenReturn(ApiResponseDTO.<DirectConversationResponse>builder().data(null).build());
    assertNull(conversationService.getDirectConversationId(1L));
  }

  @Test
  void getDirectConversationId_FeignException_ReturnsNull() {
    when(conversationClient.getDirectConversation(1L)).thenThrow(FeignException.NotFound.class);
    assertNull(conversationService.getDirectConversationId(1L));
  }

  @Test
  void getDirectConversationId_RuntimeException_ReturnsNull() {
    when(conversationClient.getDirectConversation(1L)).thenThrow(new RuntimeException("err"));

    assertNull(conversationService.getDirectConversationId(1L));
  }

  @Test
  void getDirectConversationIds_NullList_ReturnsEmpty() {
    assertTrue(conversationService.getDirectConversationIds(null).isEmpty());
  }

  @Test
  void getDirectConversationIds_EmptyList_ReturnsEmpty() {
    assertTrue(conversationService.getDirectConversationIds(List.of()).isEmpty());
  }

  @Test
  void getDirectConversationIds_WithValidList_ReturnsMap() {
    DirectConversationBatchItemResponse item = new DirectConversationBatchItemResponse();
    item.setUserId(1L);

    item.setConversationId("conv-1");
    DirectConversationBatchResponse batch = new DirectConversationBatchResponse();
    batch.setItems(List.of(item));
    when(conversationClient.getDirectConversationsBatch(any()))
        .thenReturn(ApiResponseDTO.<DirectConversationBatchResponse>builder().data(batch).build());
    assertEquals("conv-1", conversationService.getDirectConversationIds(List.of(1L)).get(1L));
  }

  @Test
  void getDirectConversationIds_NullData_ReturnsEmpty() {
    when(conversationClient.getDirectConversationsBatch(any()))
        .thenReturn(ApiResponseDTO.<DirectConversationBatchResponse>builder().data(null).build());
    assertTrue(conversationService.getDirectConversationIds(List.of(1L)).isEmpty());
  }

  @Test
  void getDirectConversationIds_NullItems_ReturnsEmpty() {
    DirectConversationBatchResponse batch = new DirectConversationBatchResponse();
    batch.setItems(null);
    when(conversationClient.getDirectConversationsBatch(any()))
        .thenReturn(ApiResponseDTO.<DirectConversationBatchResponse>builder().data(batch).build());
    assertTrue(conversationService.getDirectConversationIds(List.of(1L)).isEmpty());
  }

  @Test
  void getDirectConversationIds_FeignException_ReturnsEmpty() {
    when(conversationClient.getDirectConversationsBatch(any()))
        .thenThrow(FeignException.NotFound.class);
    assertTrue(conversationService.getDirectConversationIds(List.of(1L)).isEmpty());
  }

  @Test
  void getDirectConversationIds_RuntimeException_ReturnsEmpty() {
    when(conversationClient.getDirectConversationsBatch(any()))
        .thenThrow(new RuntimeException("err"));
    assertTrue(conversationService.getDirectConversationIds(List.of(1L)).isEmpty());
  }
}
