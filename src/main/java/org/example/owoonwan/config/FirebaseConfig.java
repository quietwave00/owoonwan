package org.example.owoonwan.config;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    @Bean
    public FirestoreClientProvider firestoreClientProvider() {
        return new FirestoreClientProvider(firebaseProperties);
    }
}
