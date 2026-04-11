package com.bitetogether.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserState {
  FOREGROUND,
  BACKGROUND,
  OFFLINE;

  @JsonValue
  public String toJson() {
    return name().toLowerCase();
  }
}
