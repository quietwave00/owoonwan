package org.example.owoonwan.common.health;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.beans.factory.ObjectProvider;
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
    public ApiResponse<Map<String, String>> ready() {
        FirestoreReadiness firestoreReadiness = firestoreReadinessProvider.getIfAvailable();
        String firestoreStatus = firestoreReadiness == null
                ? "DISABLED"
                : (firestoreReadiness.isReady() ? "UP" : "DOWN");
        return ApiResponse.ok(Map.of(
                "status", "READY",
                "firestore", firestoreStatus
        ));
    }
}
