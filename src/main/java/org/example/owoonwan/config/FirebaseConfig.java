package org.example.owoonwan.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]+$");

    private final FirebaseProperties firebaseProperties;

    @Bean
    public Firestore firestore() throws IOException {
        String credentials = firebaseProperties.getCredentials();
        if (credentials == null || credentials.isBlank()) {
            throw new IllegalStateException(
                    "Missing Firebase credentials. " +
                            "Set FIREBASE_SERVICE_ACCOUNT_JSON (raw JSON) or GOOGLE_APPLICATION_CREDENTIALS (file path)."
            );
        }

        GoogleCredentials googleCredentials;
        try (InputStream inputStream = openCredentialsStream(credentials)) {
            googleCredentials = GoogleCredentials.fromStream(inputStream);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(googleCredentials)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }

    private InputStream openCredentialsStream(String rawCredentials) throws IOException {
        String trimmed = rawCredentials.trim();
        if (trimmed.startsWith("{")) {
            return new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8));
        }

        String decodedCredentials = decodeBase64Credentials(trimmed);
        if (decodedCredentials != null) {
            return new ByteArrayInputStream(decodedCredentials.getBytes(StandardCharsets.UTF_8));
        }

        Path path = Path.of(trimmed);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Firebase credentials path does not exist: " + path);
        }

        return Files.newInputStream(path);
    }

    private String decodeBase64Credentials(String value) {
        if (!BASE64_PATTERN.matcher(value).matches()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getMimeDecoder().decode(value);
            String decodedValue = new String(decoded, StandardCharsets.UTF_8).trim();
            return decodedValue.startsWith("{") ? decodedValue : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
