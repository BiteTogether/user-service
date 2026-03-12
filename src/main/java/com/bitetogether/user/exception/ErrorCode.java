package com.bitetogether.user.exception;

import static com.bitetogether.common.enums.ApiResponseStatus.getDefaultMessage;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.BaseErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode implements BaseErrorCode {
  INVALID_FIREBASE_TOKEN(ApiResponseStatus.UNAUTHORIZED, "Firebase ID token is invalid or expired"),
  USER_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "User is not found"),
  USER_EXISTED(ApiResponseStatus.CONFLICT, "User is already existed"),
  USERNAME_EXISTED(ApiResponseStatus.CONFLICT, "This username is already existed"),
  INVALID_USERNAME_FORMAT(
      ApiResponseStatus.BAD_REQUEST,
      "Username must be 6-20 characters and contain only letters, numbers, dots, and underscores"),
  PHONE_EXISTED(ApiResponseStatus.CONFLICT, "This phone number is already existed"),
  INVALID_KEYWORD(ApiResponseStatus.BAD_REQUEST, "Invalid search keyword"),

  FRIEND_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "You don't have this friend"),
  ALREADY_FRIENDS(ApiResponseStatus.CONFLICT, "You are already friends"),

  REFRESH_TOKEN_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Refresh token is not found or expired"),
  DEVICE_TOKEN_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Device token is not found or expired"),
  REFRESH_TOKEN_ERROR(
      ApiResponseStatus.INTERNAL_SERVER_ERROR, "Error occurred during refresh token cleanup"),

  INVALID_FRIEND_REQUEST(
      ApiResponseStatus.BAD_REQUEST, "You can't send friend request to yourself"),
  FRIEND_REQUEST_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Friend request not found"),
  FRIEND_REQUEST_ALREADY_EXISTS(ApiResponseStatus.CONFLICT, "Friend request already exists"),

  // File upload errors
  FILE_EMPTY(ApiResponseStatus.BAD_REQUEST, "File is empty"),
  FILE_TOO_LARGE(ApiResponseStatus.BAD_REQUEST, "File size exceeds maximum limit (5MB)"),
  INVALID_FILE_TYPE(ApiResponseStatus.BAD_REQUEST, "Invalid file type. Only images are allowed"),
  FILE_UPLOAD_ERROR(ApiResponseStatus.INTERNAL_SERVER_ERROR, "Error occurred during file upload"),
  FILE_DELETE_ERROR(ApiResponseStatus.INTERNAL_SERVER_ERROR, "Error occurred during file deletion"),
  AVATAR_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Avatar not found"),
  ;

  ApiResponseDTO<Void> response;

  ErrorCode(ApiResponseStatus status, String message) {
    this.response =
        ApiResponseDTO.<Void>builder().status(status.getCode()).message(message).data(null).build();
  }

  public String getMessage() {
    String defaultMessage = getDefaultMessage(response.getStatus());
    String message = response.getMessage();
    return message.isEmpty() ? defaultMessage : message;
  }
}
