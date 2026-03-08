package com.bitetogether.user.service;

import com.google.firebase.auth.FirebaseToken;

public interface FirebaseAuthService {
  FirebaseToken verifyIdToken(String idToken);
}
