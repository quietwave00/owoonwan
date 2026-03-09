package org.example.owoonwan.session.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FirestoreLoginLockRepository implements LoginLockRepository {

    private final Firestore firestore;

    @Override
    public void acquire(String loginId, Instant now, Instant expiresAt) {
        FirestoreAwait.get(firestore.runTransaction(transaction -> acquireLock(transaction, loginId, now, expiresAt)));
    }

    @Override
    public void release(String loginId) {
        FirestoreAwait.get(lockRef(loginId).delete());
    }

    private Void acquireLock(Transaction transaction, String loginId, Instant now, Instant expiresAt) throws Exception {
        DocumentReference reference = lockRef(loginId);
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

    private DocumentReference lockRef(String loginId) {
        return firestore.collection("loginLocks").document(loginId);
    }
}
