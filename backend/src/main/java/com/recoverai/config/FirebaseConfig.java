package com.recoverai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "firestore")
public class FirebaseConfig {
    @Bean FirebaseApp firebaseApp(RecoveryProperties properties) throws Exception {
        String path = properties.firebase().serviceAccountPath();
        FirebaseOptions.Builder options = FirebaseOptions.builder();
        if (properties.firebase().projectId() != null && !properties.firebase().projectId().isBlank()) {
            options.setProjectId(properties.firebase().projectId());
        }
        if (path == null || path.isBlank()) options.setCredentials(GoogleCredentials.getApplicationDefault());
        else try (InputStream credentials = new FileInputStream(path)) { options.setCredentials(GoogleCredentials.fromStream(credentials)); }
        return FirebaseApp.initializeApp(options.build());
    }
    @Bean Firestore firestore(FirebaseApp app) { return FirestoreClient.getFirestore(app); }
    @Bean FirebaseAuth firebaseAuth(FirebaseApp app) { return FirebaseAuth.getInstance(app); }
}
