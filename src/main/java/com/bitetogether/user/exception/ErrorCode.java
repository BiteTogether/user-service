package com.bitetogether.user.exception;

import static com.bitetogether.common.enums.ApiResponseStatus.getDefaultMessage;

import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.BaseErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode implements BaseErrorCode {
  UNAUTHORIZED_LOGIN(ApiResponseStatus.UNAUTHORIZED, "Your username or password is not correct"),
  USER_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "User is not found"),
  USER_EXISTED(ApiResponseStatus.CONFLICT, "User is already existed"),
  USERNAME_EXISTED(ApiResponseStatus.CONFLICT, "This username is already existed"),
  EMAIL_EXISTED(ApiResponseStatus.CONFLICT, "This email is already existed"),
  PHONE_EXISTED(ApiResponseStatus.CONFLICT, "This phone number is already existed"),

  FRIEND_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "You don't have this friend"),
  ALREADY_FRIENDS(ApiResponseStatus.CONFLICT, "You are already friends"),

  INVALID_FRIEND_REQUEST(ApiResponseStatus.BAD_REQUEST, "You can't send friend request to yourself"),
  FRIEND_REQUEST_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Friend request not found"),
  FRIEND_REQUEST_ALREADY_EXISTS(ApiResponseStatus.CONFLICT, "Friend request already exists"),

  ;

  ApiResponse<Void> response;

  ErrorCode(ApiResponseStatus status, String message) {
    this.response =
        ApiResponse.<Void>builder().status(status.getCode()).message(message).data(null).build();
  }

  public String getMessage() {
    String defaultMessage = getDefaultMessage(response.getStatus());
    String message = response.getMessage();
    return message.isEmpty() ? defaultMessage : message;
  }
}
