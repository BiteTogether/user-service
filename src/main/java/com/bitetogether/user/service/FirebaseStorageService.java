package com.bitetogether.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface FirebaseStorageService {

  String uploadFile(MultipartFile file, String folder);

  boolean deleteFile(String fileUrl);

  String uploadAvatar(MultipartFile file, Long userId);
}
