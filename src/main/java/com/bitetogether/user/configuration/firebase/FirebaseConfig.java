package com.bitetogether.user.configuration.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

  private final FirebaseProperties firebaseProperties;

  @Bean
  public FirebaseApp initializeFirebase() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      FileInputStream serviceAccount = new FileInputStream(firebaseProperties.getCredentialsPath());

      FirebaseOptions options =
          FirebaseOptions.builder()
              .setCredentials(GoogleCredentials.fromStream(serviceAccount))
              .setStorageBucket(firebaseProperties.getStorageBucket())
              .build();

      FirebaseApp app = FirebaseApp.initializeApp(options);
      log.info("Firebase application has been initialized successfully");
      return app;
    }
    return FirebaseApp.getInstance();
  }

  @Bean
  public Storage storage() throws IOException {
    FileInputStream serviceAccount = new FileInputStream(firebaseProperties.getCredentialsPath());
    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
    return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
  }
}
