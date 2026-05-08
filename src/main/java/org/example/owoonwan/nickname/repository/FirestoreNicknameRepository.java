package org.example.owoonwan.nickname.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.user.domain.UserStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FirestoreNicknameRepository implements NicknameRepository {

    private final FirestoreClientProvider firestoreClientProvider;

    @Override
    public String create(String display, Instant now) {
        String nicknameId = UUID.randomUUID().toString();
        firestoreClientProvider.execute("create nickname", firestore -> {
            DocumentReference document = firestore.collection("nicknames").document(nicknameId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("display", display);
            payload.put("isActive", true);
            payload.put("assignedTo", null);
            payload.put("createdAt", Date.from(now));
            payload.put("updatedAt", Date.from(now));
            return document.set(payload);
        });
        return nicknameId;
    }

    @Override
    public Optional<Nickname> findById(String nicknameId) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("find nickname by id",
                firestore -> firestore.collection("nicknames").document(nicknameId).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(toNickname(snapshot));
    }

    @Override
    public List<Nickname> findAll() {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find all nicknames",
                firestore -> firestore.collection("nicknames").orderBy("display").get()).getDocuments();
        return documents.stream().map(this::toNickname).toList();
    }

    @Override
    public List<Nickname> findAllActive() {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find active nicknames",
                firestore -> firestore.collection("nicknames")
                        .whereEqualTo("isActive", true)
                        .orderBy("display")
                        .get()).getDocuments();
        return documents.stream().map(this::toNickname).toList();
    }

    @Override
    public Nickname update(String nicknameId, String display, Boolean isActive, Instant now) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("get nickname for update",
                firestore -> firestore.collection("nicknames").document(nicknameId).get());
        if (!snapshot.exists()) {
            throw new BusinessException(ErrorCode.NICKNAME_NOT_FOUND);
        }

        String updatedDisplay = display == null || display.isBlank() ? snapshot.getString("display") : display.trim();

        if (display != null && !display.isBlank()) {
            firestoreClientProvider.execute("update nickname display",
                    firestore -> firestore.collection("nicknames").document(nicknameId).update("display", updatedDisplay));
            syncUserNicknameDisplay(nicknameId, updatedDisplay);
        }
        if (isActive != null) {
            firestoreClientProvider.execute("update nickname active flag",
                    firestore -> firestore.collection("nicknames").document(nicknameId).update("isActive", isActive));
        }
        firestoreClientProvider.execute("update nickname timestamp",
                firestore -> firestore.collection("nicknames").document(nicknameId).update("updatedAt", Date.from(now)));
        return toNickname(firestoreClientProvider.execute("reload nickname",
                firestore -> firestore.collection("nicknames").document(nicknameId).get()));
    }

    @Override
    public void assignNicknameToUserFixedOnce(String nicknameId, String userId, Instant now) {
        firestoreClientProvider.execute("assign nickname to user",
                firestore -> firestore.runTransaction(transaction -> assignNicknameToUser(firestore, transaction, nicknameId, userId, now)));
    }

    @Override
    public void clearAssignment(String userId, Instant now) {
        List<QueryDocumentSnapshot> snapshots = firestoreClientProvider.execute("find nickname assignments",
                firestore -> firestore.collection("nicknames").whereEqualTo("assignedTo", userId).get()).getDocuments();

        for (QueryDocumentSnapshot snapshot : snapshots) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedTo", null);
            updates.put("updatedAt", Date.from(now));
            firestoreClientProvider.execute("clear nickname assignment",
                    firestore -> firestore.collection("nicknames").document(snapshot.getId()).update(updates));
        }

        firestoreClientProvider.execute("clear user nickname info",
                firestore -> firestore.collection("users").document(userId).update(Map.of(
                        "nicknameId", null,
                        "nicknameDisplay", null
                )));
    }

    private Void assignNicknameToUser(com.google.cloud.firestore.Firestore firestore,
                                      Transaction transaction,
                                      String nicknameId,
                                      String userId,
                                      Instant now) throws Exception {
        DocumentReference userRef = firestore.collection("users").document(userId);
        DocumentReference nicknameRef = firestore.collection("nicknames").document(nicknameId);
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
        transaction.update(userRef, Map.of(
                "nicknameId", nicknameId,
                "nicknameDisplay", nicknameSnapshot.getString("display")
        ));
        transaction.update(nicknameRef, Map.of("assignedTo", userId, "updatedAt", Date.from(now)));

        return null;
    }

    private void syncUserNicknameDisplay(String nicknameId, String display) {
        List<QueryDocumentSnapshot> users = firestoreClientProvider.execute("find users by nickname id",
                firestore -> firestore.collection("users")
                        .whereEqualTo("nicknameId", nicknameId)
                        .get()).getDocuments();

        for (QueryDocumentSnapshot user : users) {
            firestoreClientProvider.execute("sync user nickname display",
                    firestore -> firestore.collection("users").document(user.getId()).update("nicknameDisplay", display));
        }
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
