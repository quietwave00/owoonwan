package org.example.owoonwan.session.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FirestoreLoginLockRepository implements LoginLockRepository {

    private final FirestoreClientProvider firestoreClientProvider;

    @Override
    public void acquire(String loginId, Instant now, Instant expiresAt) {
        firestoreClientProvider.execute("acquire login lock",
                firestore -> firestore.runTransaction(transaction -> acquireLock(firestore, transaction, loginId, now, expiresAt)));
    }

    @Override
    public void release(String loginId) {
        firestoreClientProvider.execute("release login lock",
                firestore -> firestore.collection("loginLocks").document(loginId).delete());
    }

    private Void acquireLock(com.google.cloud.firestore.Firestore firestore,
                             Transaction transaction,
                             String loginId,
                             Instant now,
                             Instant expiresAt) throws Exception {
        DocumentReference reference = firestore.collection("loginLocks").document(loginId);
        DocumentSnapshot snapshot = transaction.get(reference).get();
        if (snapshot.exists()) {
            Timestamp lockExpiresAt = snapshot.getTimestamp("expiresAt");
            if (lockExpiresAt != null && lockExpiresAt.toDate().toInstant().isAfter(now)) {
                throw new BusinessException(ErrorCode.SESSION_LOCK_CONFLICT);
            }
        }
        transaction.set(reference, Map.of(
                "lockedAt", Date.from(now),
                "expiresAt", Date.from(expiresAt)
        ));
        return null;
    }
}
