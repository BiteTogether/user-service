package com.bitetogether.user.service.impl;

import com.bitetogether.common.exception.AppException;
import com.bitetogether.user.exception.ErrorCode;
import com.bitetogether.user.service.FirebaseAuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FirebaseAuthServiceImpl implements FirebaseAuthService {

  @Override
  public FirebaseToken verifyIdToken(String idToken) {
    try {
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      log.info(
          "Firebase token verified successfully for user: {}, phone: {}",
          decodedToken.getUid(),
          decodedToken.getClaims().get("phone_number"));
      return decodedToken;
    } catch (FirebaseAuthException e) {
      log.error("Firebase token verification failed: {}", e.getMessage());
      throw new AppException(ErrorCode.INVALID_FIREBASE_TOKEN);
    } catch (IllegalArgumentException e) {
      log.error("Invalid Firebase token format: {}", e.getMessage());
      throw new AppException(ErrorCode.INVALID_FIREBASE_TOKEN);
    }
  }
}
