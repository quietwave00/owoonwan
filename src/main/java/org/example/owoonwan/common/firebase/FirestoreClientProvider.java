package org.example.owoonwan.common.firebase;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.config.FirebaseProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
public class FirestoreClientProvider {

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]+$");
    private static final String FIREBASE_APP_NAME = "owoonwan";
    private static final long FIRESTORE_TIMEOUT_SECONDS = 10;

    private final FirebaseProperties firebaseProperties;

    private volatile Firestore firestore;

    public FirestoreClientProvider(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    public Firestore getFirestore() {
        Firestore current = firestore;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (firestore == null) {
                firestore = createFirestore();
            }
            return firestore;
        }
    }

    public synchronized void reset() {
        closeQuietly(firestore);
        firestore = null;

        FirebaseApp existingApp = findFirebaseApp();
        if (existingApp != null) {
            existingApp.delete();
        }
    }

    public <T> T execute(String operationName, Function<Firestore, ApiFuture<T>> operation) {
        return execute(operationName, operation, true);
    }

    private <T> T execute(String operationName, Function<Firestore, ApiFuture<T>> operation, boolean retryable) {
        ApiFuture<T> future = operation.apply(getFirestore());
        try {
            return future.get(FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            log.error("Firestore operation interrupted. operation={}", operationName, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore operation was interrupted.");
        } catch (TimeoutException exception) {
            future.cancel(true);
            if (retryable) {
                log.warn("Firestore operation timed out. Resetting client and retrying once. operation={}", operationName, exception);
                reset();
                return execute(operationName, operation, false);
            }
            log.error("Firestore operation timed out after retry. operation={}", operationName, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore operation timed out.");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            if (retryable && isReconnectable(cause)) {
                log.warn(
                        "Firestore operation failed with reconnectable cause. Resetting client and retrying once. operation={}, causeType={}",
                        operationName,
                        cause.getClass().getName(),
                        cause
                );
                reset();
                return execute(operationName, operation, false);
            }
            log.error(
                    "Firestore operation failed. operation={}, causeType={}, message={}",
                    operationName,
                    cause.getClass().getName(),
                    cause.getMessage(),
                    cause
            );
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore operation failed.");
        }
    }

    private boolean isReconnectable(Throwable cause) {
        String causeType = cause.getClass().getName();
        String message = cause.getMessage();
        if (causeType.startsWith("io.grpc.")) {
            return true;
        }
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase();
        return normalized.contains("connection")
                || normalized.contains("reset")
                || normalized.contains("closed")
                || normalized.contains("unavailable")
                || normalized.contains("deadline exceeded")
                || normalized.contains("timed out");
    }

    private Firestore createFirestore() {
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
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Firebase credentials.", exception);
        }

        FirebaseApp existingApp = findFirebaseApp();
        if (existingApp != null) {
            existingApp.delete();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(googleCredentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
        return FirestoreClient.getFirestore(app);
    }

    private FirebaseApp findFirebaseApp() {
        return FirebaseApp.getApps().stream()
                .filter(app -> FIREBASE_APP_NAME.equals(app.getName()))
                .findFirst()
                .orElse(null);
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

    private void closeQuietly(Firestore current) {
        if (current == null) {
            return;
        }

        try {
            current.close();
        } catch (Exception exception) {
            log.warn("Failed to close Firestore client cleanly", exception);
        }
    }
}
