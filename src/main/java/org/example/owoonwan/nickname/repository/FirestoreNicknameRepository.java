package org.example.owoonwan.nickname.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.user.domain.UserStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FirestoreNicknameRepository implements NicknameRepository {

    private final Firestore firestore;

    @Override
    public String create(String display, Instant now) {
        DocumentReference document = nicknames().document();
        Map<String, Object> payload = new HashMap<>();
        payload.put("display", display);
        payload.put("isActive", true);
        payload.put("assignedTo", null);
        payload.put("createdAt", Date.from(now));
        payload.put("updatedAt", Date.from(now));
        FirestoreAwait.get(document.set(payload));
        return document.getId();
    }

    @Override
    public Optional<Nickname> findById(String nicknameId) {
        DocumentSnapshot snapshot = FirestoreAwait.get(nicknames().document(nicknameId).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(toNickname(snapshot));
    }

    @Override
    public List<Nickname> findAll() {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                nicknames().orderBy("display").get()
        ).getDocuments();
        return documents.stream().map(this::toNickname).toList();
    }

    @Override
    public List<Nickname> findAllActive() {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                nicknames().whereEqualTo("isActive", true).orderBy("display").get()
        ).getDocuments();
        return documents.stream().map(this::toNickname).toList();
    }

    @Override
    public Nickname update(String nicknameId, String display, Boolean isActive, Instant now) {
        DocumentReference nicknameRef = nicknames().document(nicknameId);
        DocumentSnapshot snapshot = FirestoreAwait.get(nicknameRef.get());
        if (!snapshot.exists()) {
            throw new BusinessException(ErrorCode.NICKNAME_NOT_FOUND);
        }

        if (display != null && !display.isBlank()) {
            FirestoreAwait.get(nicknameRef.update("display", display.trim()));
        }
        if (isActive != null) {
            FirestoreAwait.get(nicknameRef.update("isActive", isActive));
        }
        FirestoreAwait.get(nicknameRef.update("updatedAt", Date.from(now)));
        return toNickname(FirestoreAwait.get(nicknameRef.get()));
    }

    @Override
    public void assignNicknameToUserFixedOnce(String nicknameId, String userId, Instant now) {
        FirestoreAwait.get(firestore.runTransaction(transaction -> assignNicknameToUser(transaction, nicknameId, userId, now)));
    }

    @Override
    public void clearAssignment(String userId, Instant now) {
        List<QueryDocumentSnapshot> snapshots = FirestoreAwait.get(
                nicknames().whereEqualTo("assignedTo", userId).get()
        ).getDocuments();

        for (QueryDocumentSnapshot snapshot : snapshots) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedTo", null);
            updates.put("updatedAt", Date.from(now));
            FirestoreAwait.get(snapshot.getReference().update(updates));
        }
    }

    private Void assignNicknameToUser(
            Transaction transaction,
            String nicknameId,
            String userId,
            Instant now
    ) throws Exception {
        DocumentReference userRef = firestore.collection("users").document(userId);
        DocumentReference nicknameRef = nicknames().document(nicknameId);
        DocumentSnapshot userSnapshot = transaction.get(userRef).get();
        DocumentSnapshot nicknameSnapshot = transaction.get(nicknameRef).get();

        if (!userSnapshot.exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String status = userSnapshot.getString("status");
        if (!UserStatus.ACTIVE.name().equals(status)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        String fixedNicknameId = userSnapshot.getString("nicknameId");
        if (fixedNicknameId != null && !fixedNicknameId.isBlank()) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_FIXED);
        }

        if (!nicknameSnapshot.exists()) {
            throw new BusinessException(ErrorCode.NICKNAME_NOT_FOUND);
        }

        Boolean active = nicknameSnapshot.getBoolean("isActive");
        if (!Boolean.TRUE.equals(active)) {
            throw new BusinessException(ErrorCode.NICKNAME_INACTIVE);
        }

        String assignedTo = nicknameSnapshot.getString("assignedTo");
        if (assignedTo != null && !assignedTo.isBlank()) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_ASSIGNED);
        }
        transaction.update(userRef, "nicknameId", nicknameId);
        transaction.update(nicknameRef, Map.of(
                "assignedTo", userId,
                "updatedAt", Date.from(now)
        ));

        return null;
    }

    private CollectionReference nicknames() {
        return firestore.collection("nicknames");
    }

    private Nickname toNickname(DocumentSnapshot snapshot) {
        Timestamp createdAt = snapshot.getTimestamp("createdAt");
        Timestamp updatedAt = snapshot.getTimestamp("updatedAt");

        return new Nickname(
                snapshot.getId(),
                snapshot.getString("display"),
                Boolean.TRUE.equals(snapshot.getBoolean("isActive")),
                snapshot.getString("assignedTo"),
                createdAt == null ? null : createdAt.toDate().toInstant(),
                updatedAt == null ? null : updatedAt.toDate().toInstant()
        );
    }
}
