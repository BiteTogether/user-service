package com.bitetogether.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum UserState {
  FOREGROUND,
  BACKGROUND,
  OFFLINE;

  @JsonValue
  public String toJson() {
    return name().toLowerCase();
  }

  @JsonCreator
  public static UserState fromJson(String value) {
    return Arrays.stream(values())
        .filter(state -> state.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid user state: " + value));
  }
}

