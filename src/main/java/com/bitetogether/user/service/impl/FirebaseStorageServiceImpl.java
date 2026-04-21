package com.bitetogether.user.service.impl;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.configuration.firebase.FirebaseProperties;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.service.FirebaseStorageService;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

  Storage storage;
  FirebaseProperties firebaseProperties;

  // String literal constants
  private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
  private static final String IMAGE_JPEG = "image/jpeg";
  private static final String IMAGE_JPG = "image/jpg";
  private static final String IMAGE_PNG = "image/png";
  private static final String IMAGE_GIF = "image/gif";
  private static final String IMAGE_WEBP = "image/webp";
  private static final String EXT_JPG = "jpg";
  private static final String EXT_JPEG = "jpeg";
  private static final String EXT_PNG = "png";
  private static final String EXT_GIF = "gif";
  private static final String EXT_WEBP = "webp";

  // Content type constants - using already defined constants
  private static final List<String> ALLOWED_IMAGE_TYPES =
      Arrays.asList(IMAGE_JPEG, IMAGE_PNG, IMAGE_JPG, IMAGE_GIF, IMAGE_WEBP);
  private static final List<String> ALLOWED_EXTENSIONS =
      Arrays.asList(EXT_JPG, EXT_JPEG, EXT_PNG, EXT_GIF, EXT_WEBP);

  private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
  private static final String FIREBASE_STORAGE_URL_TEMPLATE =
      "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media";
  private static final String URL_DELIMITER_O = "/o/";
  private static final String URL_DELIMITER_QUERY = "\\?";
  private static final String APPSPOT_DOMAIN = ".appspot.com";
  private static final String FIREBASESTORAGE_DOMAIN = ".firebasestorage.app";
  private static final String FOLDER_SEPARATOR = "/";
  private static final String FILE_EXTENSION_DOT = ".";
  private static final String CACHE_CONTROL = "public, max-age=86400";

  @Override
  public String uploadFile(MultipartFile file, String folder) {
    validateFile(file);

    try {
      String fileName = generateFileName(file, folder);
      String bucketName = firebaseProperties.getStorageBucket();
      String contentType = determineContentType(file);

      logUploadInfo(file, bucketName, contentType);

      BlobInfo blobInfo = createBlobInfo(bucketName, fileName, contentType);
      storage.create(blobInfo, file.getBytes());

      String publicUrl = generatePublicUrl(bucketName, fileName);
      log.info("File uploaded successfully: {}", publicUrl);

      return publicUrl;
    } catch (com.google.cloud.storage.StorageException e) {
      handleStorageException(e);
      throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
    } catch (IOException e) {
      log.error("Error uploading file to Firebase Storage", e);
      throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
    }
  }

  @Override
  public boolean deleteFile(String fileUrl) {
    try {
      String fileName = extractFileNameFromUrl(fileUrl);
      String bucketName = firebaseProperties.getStorageBucket();

      BlobId blobId = BlobId.of(bucketName, fileName);
      boolean deleted = storage.delete(blobId);

      logDeletionResult(deleted, fileName);
      return deleted;
    } catch (Exception e) {
      log.error("Error deleting file from Firebase Storage", e);
      return false;
    }
  }

  @Override
  public String uploadAvatar(MultipartFile file, Long userId) {
    return uploadFile(file, "avatars/user_" + userId);
  }

  private void validateFile(MultipartFile file) {
    log.info("Starting file validation...");

    validateFileExists(file);
    logFileDetails(file);
    validateFileSize(file);
    validateFileType(file);

    log.info("File validation passed successfully");
  }

  private void validateFileExists(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      log.error("File is null or empty");
      throw new AppException(ErrorCode.FILE_EMPTY);
    }
  }

  private void logFileDetails(MultipartFile file) {
    log.info(
        "File details - Name: {}, Size: {} bytes, Content-Type: '{}'",
        file.getOriginalFilename(),
        file.getSize(),
        file.getContentType());
  }

  private void validateFileSize(MultipartFile file) {
    if (file.getSize() > MAX_FILE_SIZE) {
      log.error("File size {} exceeds maximum limit {}", file.getSize(), MAX_FILE_SIZE);
      throw new AppException(ErrorCode.FILE_TOO_LARGE);
    }
  }

  private void validateFileType(MultipartFile file) {
    String contentType = file.getContentType();

    if (isGenericContentType(contentType)) {
      validateByFileExtension(file.getOriginalFilename(), contentType);
    } else {
      validateByContentType(contentType);
    }
  }

  private boolean isGenericContentType(String contentType) {
    return contentType == null
        || contentType.isEmpty()
        || CONTENT_TYPE_OCTET_STREAM.equalsIgnoreCase(contentType);
  }

  private void validateByFileExtension(String filename, String contentType) {
    log.warn(
        "Content type is null, empty, or generic ({}). Falling back to file extension validation",
        contentType);

    String extension = extractFileExtension(filename);
    log.info("Validating by file extension: '{}'", extension);

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      log.error(
          "Invalid file extension: '{}'. Allowed extensions: {}", extension, ALLOWED_EXTENSIONS);
      throw new AppException(ErrorCode.INVALID_FILE_TYPE);
    }

    log.info("File extension '{}' is valid", extension);
  }

  private void validateByContentType(String contentType) {
    log.info(
        "Validating content type: '{}' against allowed types: {}",
        contentType,
        ALLOWED_IMAGE_TYPES);

    String normalizedContentType = contentType.trim().toLowerCase();

    if (ALLOWED_IMAGE_TYPES.stream()
        .noneMatch(type -> type.equalsIgnoreCase(normalizedContentType))) {
      log.error("Invalid file type: '{}'. Allowed types are: {}", contentType, ALLOWED_IMAGE_TYPES);
      throw new AppException(ErrorCode.INVALID_FILE_TYPE);
    }

    log.info("Content type '{}' is valid", contentType);
  }

  private String extractFileExtension(String filename) {
    if (filename == null || !filename.contains(FILE_EXTENSION_DOT)) {
      log.error("Cannot determine file type from extension");
      throw new AppException(ErrorCode.INVALID_FILE_TYPE);
    }
    return filename.substring(filename.lastIndexOf(FILE_EXTENSION_DOT) + 1).toLowerCase();
  }

  private String generateFileName(MultipartFile file, String folder) {
    String originalFileName = file.getOriginalFilename();
    String extension = "";
    if (originalFileName != null && originalFileName.contains(FILE_EXTENSION_DOT)) {
      extension = originalFileName.substring(originalFileName.lastIndexOf(FILE_EXTENSION_DOT));
    }
    String uniqueId = UUID.randomUUID().toString();
    return folder + FOLDER_SEPARATOR + uniqueId + extension;
  }

  private String determineContentType(MultipartFile file) {
    String contentType = file.getContentType();

    if (contentType != null
        && !contentType.isEmpty()
        && !CONTENT_TYPE_OCTET_STREAM.equalsIgnoreCase(contentType)) {
      return contentType;
    }

    return determineContentTypeFromExtension(file.getOriginalFilename());
  }

  private String determineContentTypeFromExtension(String filename) {
    if (filename == null || !filename.contains(FILE_EXTENSION_DOT)) {
      return CONTENT_TYPE_OCTET_STREAM;
    }

    String extension =
        filename.substring(filename.lastIndexOf(FILE_EXTENSION_DOT) + 1).toLowerCase();

    return switch (extension) {
      case EXT_JPG, EXT_JPEG -> IMAGE_JPEG;
      case EXT_PNG -> IMAGE_PNG;
      case EXT_GIF -> IMAGE_GIF;
      case EXT_WEBP -> IMAGE_WEBP;
      default -> CONTENT_TYPE_OCTET_STREAM;
    };
  }

  private BlobInfo createBlobInfo(String bucketName, String fileName, String contentType) {
    BlobId blobId = BlobId.of(bucketName, fileName);
    return BlobInfo.newBuilder(blobId)
        .setContentType(contentType)
        .setCacheControl(CACHE_CONTROL)
        .build();
  }

  private String generatePublicUrl(String bucketName, String fileName) {
    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    return String.format(FIREBASE_STORAGE_URL_TEMPLATE, bucketName, encodedFileName);
  }

  private void logUploadInfo(MultipartFile file, String bucketName, String contentType) {
    log.info("Using content type: {} for file: {}", contentType, file.getOriginalFilename());
    log.info("Uploading to Firebase Storage bucket: {}", bucketName);
  }

  private void handleStorageException(com.google.cloud.storage.StorageException e) {
    log.error("Firebase Storage error - Code: {}, Message: {}", e.getCode(), e.getMessage());
    if (e.getCode() == 404) {
      String projectId =
          firebaseProperties
              .getStorageBucket()
              .replace(APPSPOT_DOMAIN, "")
              .replace(FIREBASESTORAGE_DOMAIN, "");
      log.error(
          "Firebase Storage bucket '{}' does not exist. Please create it in Firebase Console: https://console.firebase.google.com/project/{}/storage",
          firebaseProperties.getStorageBucket(),
          projectId);
    }
  }

  private void logDeletionResult(boolean deleted, String fileName) {
    if (deleted) {
      log.info("File deleted successfully: {}", fileName);
    } else {
      log.warn("File not found for deletion: {}", fileName);
    }
  }

  private String extractFileNameFromUrl(String fileUrl) {
    String[] parts = fileUrl.split(URL_DELIMITER_O);
    if (parts.length > 1) {
      String encodedFileName = parts[1].split(URL_DELIMITER_QUERY)[0];
      return java.net.URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8);
    }
    return "";
  }
}
