package org.example.owoonwan.common.health;

import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ConditionalOnBean(Firestore.class)
public class FirestoreReadiness implements ApplicationRunner {

    private static final int MAX_ATTEMPTS = 3;
    private static final long WARMUP_TIMEOUT_SECONDS = 15;
    private static final long RETRY_DELAY_MILLIS = 2000;

    private final Firestore firestore;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public FirestoreReadiness(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        warmUp();
        ready.set(true);
        log.info("Firestore readiness warm-up completed");
    }

    public boolean isReady() {
        return ready.get();
    }

    private void warmUp() throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                firestore.collection("_health").limit(1).get().get(WARMUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return;
            } catch (Exception exception) {
                lastException = exception;
                log.warn("Firestore warm-up failed on attempt {}/{}", attempt, MAX_ATTEMPTS, exception);
                if (attempt < MAX_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MILLIS);
                }
            }
        }

        throw new IllegalStateException("Firestore warm-up failed during startup", lastException);
    }
}
