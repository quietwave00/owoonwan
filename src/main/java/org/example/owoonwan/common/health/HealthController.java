package org.example.owoonwan.common.health;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final ObjectProvider<FirestoreReadiness> firestoreReadinessProvider;

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<?>> ready() {
        FirestoreReadiness firestoreReadiness = firestoreReadinessProvider.getIfAvailable();
        boolean ready = firestoreReadiness == null || firestoreReadiness.isReady();
        if (!ready) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.fail("NOT_READY", "Firestore is not ready."));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "READY")));
    }
}
