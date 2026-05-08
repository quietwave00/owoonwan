package org.example.owoonwan.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ConditionalOnBean(FirestoreClientProvider.class)
public class FirestoreReadiness {

    private final FirestoreClientProvider firestoreClientProvider;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public FirestoreReadiness(FirestoreClientProvider firestoreClientProvider) {
        this.firestoreClientProvider = firestoreClientProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        refresh();
    }

    public void refresh() {
        try {
            firestoreClientProvider.execute("firestore readiness probe",
                    firestore -> firestore.collection("_health").limit(1).get());
            if (ready.compareAndSet(false, true)) {
                log.info("Firestore readiness probe succeeded");
            }
        } catch (Exception exception) {
            ready.set(false);
            log.warn("Firestore readiness probe failed", exception);
        }
    }

    public boolean isReady() {
        return ready.get();
    }
}
