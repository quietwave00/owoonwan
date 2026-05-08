package org.example.owoonwan.pledge.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
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

    private final FirestoreClientProvider firestoreClientProvider;

    @Override
    public Optional<Pledge> findByUserId(String userId) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("find pledge by user id",
                firestore -> firestore.collection("pledges").document(userId).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(toPledge(snapshot));
    }

    @Override
    public List<Pledge> findAll() {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find all pledges",
                firestore -> firestore.collection("pledges").orderBy("updatedAt").get()).getDocuments();
        return documents.stream()
                .map(this::toPledge)
                .toList();
    }

    @Override
    public Pledge save(String userId, String text, Instant now) {
        return firestoreClientProvider.execute("save pledge",
                firestore -> firestore.runTransaction(transaction -> savePledge(firestore, transaction, userId, text, now)));
    }

    @Override
    public void deleteByUserId(String userId) {
        firestoreClientProvider.execute("delete pledge",
                firestore -> firestore.collection("pledges").document(userId).delete());
    }

    private Pledge savePledge(com.google.cloud.firestore.Firestore firestore,
                              Transaction transaction,
                              String userId,
                              String text,
                              Instant now) throws Exception {
        DocumentReference pledgeRef = firestore.collection("pledges").document(userId);
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
