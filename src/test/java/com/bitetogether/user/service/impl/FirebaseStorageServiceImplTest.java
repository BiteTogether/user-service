package com.bitetogether.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.configuration.firebase.FirebaseProperties;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FirebaseStorageServiceImplTest {

  @Mock private Storage storage;
  @Mock private FirebaseProperties firebaseProperties;
  @Mock private MultipartFile file;

  @InjectMocks private FirebaseStorageServiceImpl firebaseStorageService;

  @BeforeEach
  void setUp() {}

  // ==================== uploadFile tests ====================

  @Test
  void uploadFile_WithValidJpegFile_ReturnsUrl() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "test-folder");

    assertNotNull(result);
    assertTrue(result.contains("firebasestorage.googleapis.com"));
    assertTrue(result.contains("test-bucket"));
    verify(storage).create(any(BlobInfo.class), any(byte[].class));
  }

  @Test
  void uploadFile_WithPngFile_ReturnsUrl() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(2048L);
    when(file.getContentType()).thenReturn("image/png");
    when(file.getOriginalFilename()).thenReturn("test.png");
    when(file.getBytes()).thenReturn(new byte[2048]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
    assertTrue(result.contains("test-bucket"));
  }

  @Test
  void uploadFile_WithNullFile_ThrowsException() {
    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(null, "folder"));
  }

  @Test
  void uploadFile_WithEmptyFile_ThrowsException() {
    when(file.isEmpty()).thenReturn(true);

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
    verify(storage, never()).create(any(BlobInfo.class), any(byte[].class));
  }

  @Test
  void uploadFile_WithFileTooLarge_ThrowsException() {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(6L * 1024 * 1024); // 6MB
    when(file.getOriginalFilename()).thenReturn("large.jpg");
    when(file.getContentType()).thenReturn("image/jpeg");

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
    verify(storage, never()).create(any(BlobInfo.class), any(byte[].class));
  }

  @Test
  void uploadFile_WithInvalidContentType_ThrowsException() {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("application/pdf");
    when(file.getOriginalFilename()).thenReturn("test.pdf");

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
    verify(storage, never()).create(any(BlobInfo.class), any(byte[].class));
  }

  @Test
  void uploadFile_WithNullContentType_FallsBackToExtension() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn(null);
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WithOctetStreamContentType_FallsBackToExtension() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("application/octet-stream");
    when(file.getOriginalFilename()).thenReturn("test.png");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WithInvalidExtensionAndNullContentType_ThrowsException() {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn(null);
    when(file.getOriginalFilename()).thenReturn("test.pdf");

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
  }

  @Test
  void uploadFile_WithNoExtensionAndNullContentType_ThrowsException() {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn(null);
    when(file.getOriginalFilename()).thenReturn("testfile");

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
  }

  @Test
  void uploadFile_WithGifFile_ReturnsUrl() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/gif");
    when(file.getOriginalFilename()).thenReturn("test.gif");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WithWebpFile_ReturnsUrl() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/webp");
    when(file.getOriginalFilename()).thenReturn("test.webp");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WhenStorageException_ThrowsAppException() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class)))
        .thenThrow(new StorageException(500, "Storage error"));

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
  }

  @Test
  void uploadFile_WhenStorageException404_ThrowsAppException() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket.appspot.com");
    when(storage.create(any(BlobInfo.class), any(byte[].class)))
        .thenThrow(new StorageException(404, "Bucket not found"));

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
  }

  @Test
  void uploadFile_WhenIOException_ThrowsAppException() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getBytes()).thenThrow(new IOException("IO error"));
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");

    assertThrows(AppException.class, () -> firebaseStorageService.uploadFile(file, "folder"));
  }

  @Test
  void uploadFile_WithFileNoExtension_GeneratesWithoutExtension() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("testfile");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WithNullOriginalFilename_GeneratesCorrectly() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn(null);
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  // ==================== deleteFile tests ====================

  @Test
  void deleteFile_WithValidUrl_ReturnsTrue() {
    String fileUrl =
        "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/folder%2Ffile.jpg?alt=media";
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.delete(any(BlobId.class))).thenReturn(true);

    boolean result = firebaseStorageService.deleteFile(fileUrl);

    assertTrue(result);
    verify(storage).delete(any(BlobId.class));
  }

  @Test
  void deleteFile_WhenFileNotFound_ReturnsFalse() {
    String fileUrl =
        "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/folder%2Ffile.jpg?alt=media";
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.delete(any(BlobId.class))).thenReturn(false);

    boolean result = firebaseStorageService.deleteFile(fileUrl);

    assertFalse(result);
  }

  @Test
  void deleteFile_WhenException_ReturnsFalse() {
    String fileUrl =
        "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/folder%2Ffile.jpg?alt=media";
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.delete(any(BlobId.class))).thenThrow(new RuntimeException("Error"));

    boolean result = firebaseStorageService.deleteFile(fileUrl);

    assertFalse(result);
  }

  // ==================== uploadAvatar tests ====================

  @Test
  void uploadAvatar_DelegatesToUploadFile() throws IOException {
    Long userId = 1L;
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("avatar.jpg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadAvatar(file, userId);

    assertNotNull(result);
    assertTrue(result.contains("test-bucket"));
  }

  // ==================== edge case: exactly at max size ====================

  @Test
  void uploadFile_WithExactMaxSize_Succeeds() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(5L * 1024 * 1024); // exactly 5MB
    when(file.getContentType()).thenReturn("image/jpeg");
    when(file.getOriginalFilename()).thenReturn("test.jpg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WithJpegExtensionAndNullContentType_ReturnsUrl() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn(null);
    when(file.getOriginalFilename()).thenReturn("test.jpeg");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void uploadFile_WithEmptyContentType_FallsBackToExtension() throws IOException {
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(1024L);
    when(file.getContentType()).thenReturn("");
    when(file.getOriginalFilename()).thenReturn("test.png");
    when(file.getBytes()).thenReturn(new byte[1024]);
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

    String result = firebaseStorageService.uploadFile(file, "folder");

    assertNotNull(result);
  }

  @Test
  void deleteFile_WithUrlWithoutODelimiter_HandlesGracefully() {
    String fileUrl = "https://firebasestorage.googleapis.com/v0/b/test-bucket/file.jpg";
    when(firebaseProperties.getStorageBucket()).thenReturn("test-bucket");
    when(storage.delete(any(BlobId.class))).thenReturn(true);

    boolean result = firebaseStorageService.deleteFile(fileUrl);

    assertTrue(result);
  }
}
