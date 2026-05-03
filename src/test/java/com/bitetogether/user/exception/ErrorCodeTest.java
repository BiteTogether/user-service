package com.bitetogether.user.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorCodeTest {

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void allErrorCodes_HaveNonNullResponse(ErrorCode errorCode) {
    assertNotNull(errorCode.getResponse());
    assertNotNull(errorCode.getResponse().getStatus());
    assertNotNull(errorCode.getResponse().getMessage());
  }

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void allErrorCodes_HaveNonEmptyMessage(ErrorCode errorCode) {
    String message = errorCode.getMessage();
    assertNotNull(message);
    assertFalse(message.isEmpty());
  }

  @Test
  void errorCode_ResponseDataIsNull() {
    for (ErrorCode code : ErrorCode.values()) {
      assertNull(code.getResponse().getData());
    }
  }
}
