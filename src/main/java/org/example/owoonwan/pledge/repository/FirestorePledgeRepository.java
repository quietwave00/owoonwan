package org.example.owoonwan.pledge.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.example.owoonwan.pledge.domain.Pledge;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FirestorePledgeRepository implements PledgeRepository {

    private final Firestore firestore;

    @Override
    public Optional<Pledge> findByUserId(String userId) {
        DocumentSnapshot snapshot = FirestoreAwait.get(pledges().document(userId).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(toPledge(snapshot));
    }

    @Override
    public List<Pledge> findAll() {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                pledges().orderBy("updatedAt").get()
        ).getDocuments();
        return documents.stream()
                .map(this::toPledge)
                .toList();
    }

    @Override
    public Pledge save(String userId, String text, Instant now) {
        return FirestoreAwait.get(firestore.runTransaction(transaction -> savePledge(transaction, userId, text, now)));
    }

    @Override
    public void deleteByUserId(String userId) {
        FirestoreAwait.get(pledges().document(userId).delete());
    }

    private Pledge savePledge(Transaction transaction, String userId, String text, Instant now) throws Exception {
        DocumentReference pledgeRef = pledges().document(userId);
        DocumentSnapshot snapshot = transaction.get(pledgeRef).get();
        Long currentVersion = snapshot.exists() ? snapshot.getLong("version") : null;
        int nextVersion = currentVersion == null ? 1 : currentVersion.intValue() + 1;
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "text", text,
                "updatedAt", Date.from(now),
                "version", nextVersion
        );
        transaction.set(pledgeRef, payload);
        return new Pledge(userId, text, now, nextVersion);
    }

    private CollectionReference pledges() {
        return firestore.collection("pledges");
    }

    private Pledge toPledge(DocumentSnapshot snapshot) {
        Timestamp updatedAt = snapshot.getTimestamp("updatedAt");
        Long version = snapshot.getLong("version");
        return new Pledge(
                snapshot.getString("userId"),
                snapshot.getString("text"),
                updatedAt == null ? null : updatedAt.toDate().toInstant(),
                version == null ? 0 : version.intValue()
        );
    }
}
