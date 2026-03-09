package org.example.owoonwan.session.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.example.owoonwan.session.domain.Session;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FirestoreSessionRepository implements SessionRepository {

    private final Firestore firestore;

    @Override
    public String create(String userId, String loginId, Instant now, Instant expiresAt) {
        String token = UUID.randomUUID().toString();
        FirestoreAwait.get(sessions().document(token).set(Map.of(
                "userId", userId,
                "loginId", loginId,
                "createdAt", Date.from(now),
                "lastSeenAt", Date.from(now),
                "isActive", true,
                "expiresAt", Date.from(expiresAt)
        )));
        return token;
    }

    @Override
    public Optional<Session> findByToken(String token) {
        DocumentSnapshot snapshot = FirestoreAwait.get(sessions().document(token).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(toSession(snapshot));
    }

    @Override
    public void deactivateByToken(String token, Instant now) {
        FirestoreAwait.get(sessions().document(token).update(Map.of(
                "isActive", false,
                "lastSeenAt", Date.from(now)
        )));
    }

    @Override
    public void deactivateAllByUserId(String userId, Instant now) {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                sessions().whereEqualTo("userId", userId)
                        .whereEqualTo("isActive", true)
                        .get()
        ).getDocuments();

        for (QueryDocumentSnapshot document : documents) {
            FirestoreAwait.get(document.getReference().update(Map.of(
                    "isActive", false,
                    "lastSeenAt", Date.from(now)
            )));
        }
    }

    @Override
    public void touchLastSeen(String token, Instant now) {
        FirestoreAwait.get(sessions().document(token).update("lastSeenAt", Date.from(now)));
    }

    private CollectionReference sessions() {
        return firestore.collection("sessions");
    }

    private Session toSession(DocumentSnapshot snapshot) {
        Timestamp createdAt = snapshot.getTimestamp("createdAt");
        Timestamp lastSeenAt = snapshot.getTimestamp("lastSeenAt");
        Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
        return new Session(
                snapshot.getId(),
                snapshot.getString("userId"),
                snapshot.getString("loginId"),
                Boolean.TRUE.equals(snapshot.getBoolean("isActive")),
                createdAt == null ? null : createdAt.toDate().toInstant(),
                lastSeenAt == null ? null : lastSeenAt.toDate().toInstant(),
                expiresAt == null ? null : expiresAt.toDate().toInstant()
        );
    }
}
