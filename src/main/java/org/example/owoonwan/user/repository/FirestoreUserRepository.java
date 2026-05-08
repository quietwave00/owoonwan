package org.example.owoonwan.user.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FirestoreUserRepository implements UserRepository {

    private final FirestoreClientProvider firestoreClientProvider;

    @Override
    public String create(String loginId, UserRole role, Instant now) {
        return firestoreClientProvider.execute("create user",
                firestore -> firestore.runTransaction(transaction -> createUserWithLoginLock(firestore, transaction, loginId, role, now)));
    }

    @Override
    public Optional<User> findById(String userId) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("find user by id",
                firestore -> firestore.collection("users").document(userId).get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }
        return Optional.of(toUser(snapshot));
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find user by login id",
                firestore -> firestore.collection("users").whereEqualTo("loginId", loginId).limit(1).get()).getDocuments();
        if (documents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toUser(documents.get(0)));
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        if (firestoreClientProvider.execute("check login id lock",
                firestore -> firestore.collection("loginIds").document(loginId).get()).exists()) {
            return true;
        }
        return !firestoreClientProvider.execute("check login id existence",
                firestore -> firestore.collection("users").whereEqualTo("loginId", loginId).limit(1).get()).isEmpty();
    }

    @Override
    public List<User> findByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        Map<String, DocumentReference> refsById = new LinkedHashMap<>();
        List<DocumentSnapshot> snapshots = firestoreClientProvider.execute("find users by ids",
                firestore -> {
                    for (String userId : userIds) {
                        if (userId == null || userId.isBlank() || refsById.containsKey(userId)) {
                            continue;
                        }
                        refsById.put(userId, firestore.collection("users").document(userId));
                    }
                    if (refsById.isEmpty()) {
                        return com.google.api.core.ApiFutures.immediateFuture(List.<DocumentSnapshot>of());
                    }
                    return firestore.getAll(refsById.values().toArray(DocumentReference[]::new));
                });
        Map<String, User> usersById = new HashMap<>();
        for (DocumentSnapshot snapshot : snapshots) {
            if (snapshot.exists()) {
                usersById.put(snapshot.getId(), toUser(snapshot));
            }
        }

        List<User> orderedUsers = new ArrayList<>();
        for (String userId : refsById.keySet()) {
            User user = usersById.get(userId);
            if (user != null) {
                orderedUsers.add(user);
            }
        }
        return List.copyOf(orderedUsers);
    }

    @Override
    public List<User> findAll() {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find all users",
                firestore -> firestore.collection("users").orderBy("createdAt").get()).getDocuments();
        return documents.stream().map(this::toUser).toList();
    }

    @Override
    public User updateRole(String userId, UserRole role) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("get user for role update",
                firestore -> firestore.collection("users").document(userId).get());
        if (!snapshot.exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        firestoreClientProvider.execute("update user role",
                firestore -> firestore.collection("users").document(userId).update(Map.of("role", role.name())));
        return toUser(firestoreClientProvider.execute("reload user after role update",
                firestore -> firestore.collection("users").document(userId).get()));
    }

    @Override
    public User updateKakkdugi(String userId, boolean kakkdugi) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("get user for kakkdugi update",
                firestore -> firestore.collection("users").document(userId).get());
        if (!snapshot.exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        firestoreClientProvider.execute("update user kakkdugi",
                firestore -> firestore.collection("users").document(userId).update(Map.of("kakkdugi", kakkdugi)));
        return toUser(firestoreClientProvider.execute("reload user after kakkdugi update",
                firestore -> firestore.collection("users").document(userId).get()));
    }

    @Override
    public User softDelete(String userId, Instant now) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("get user for soft delete",
                firestore -> firestore.collection("users").document(userId).get());
        if (!snapshot.exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        User user = toUser(snapshot);
        if (user.status() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", UserStatus.DELETED.name());
        updates.put("deletedAt", Date.from(now));
        updates.put("nicknameId", null);
        updates.put("nicknameDisplay", null);
        firestoreClientProvider.execute("soft delete user",
                firestore -> firestore.collection("users").document(userId).update(updates));

        return toUser(firestoreClientProvider.execute("reload user after soft delete",
                firestore -> firestore.collection("users").document(userId).get()));
    }

    @Override
    public void updateLastLoginAt(String userId, Instant now) {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("get user for last login update",
                firestore -> firestore.collection("users").document(userId).get());
        if (!snapshot.exists()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        firestoreClientProvider.execute("update last login timestamp",
                firestore -> firestore.collection("users").document(userId).update("lastLoginAt", Date.from(now)));
    }

    private String createUserWithLoginLock(com.google.cloud.firestore.Firestore firestore,
                                           Transaction transaction,
                                           String loginId,
                                           UserRole role,
                                           Instant now) throws Exception {
        DocumentReference loginIdRef = firestore.collection("loginIds").document(loginId);
        if (transaction.get(loginIdRef).get().exists()) {
            throw new BusinessException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        String userId = UUID.randomUUID().toString();
        DocumentReference userRef = firestore.collection("users").document(userId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("loginId", loginId);
        payload.put("nicknameId", null);
        payload.put("nicknameDisplay", null);
        payload.put("role", role.name());
        payload.put("status", UserStatus.ACTIVE.name());
        payload.put("createdAt", Date.from(now));
        payload.put("deletedAt", null);
        payload.put("lastLoginAt", null);
        payload.put("kakkdugi", false);
        payload.put("pledgeId", null);
        transaction.set(userRef, payload);
        transaction.set(loginIdRef, Map.of(
                "userId", userId,
                "createdAt", Date.from(now)
        ));
        return userId;
    }

    private User toUser(DocumentSnapshot snapshot) {
        Timestamp createdAt = snapshot.getTimestamp("createdAt");
        Timestamp deletedAt = snapshot.getTimestamp("deletedAt");
        Timestamp lastLoginAt = snapshot.getTimestamp("lastLoginAt");

        return new User(
                snapshot.getId(),
                snapshot.getString("loginId"),
                snapshot.getString("nicknameId"),
                snapshot.getString("nicknameDisplay"),
                UserRole.valueOf(snapshot.getString("role")),
                UserStatus.valueOf(snapshot.getString("status")),
                createdAt == null ? null : createdAt.toDate().toInstant(),
                deletedAt == null ? null : deletedAt.toDate().toInstant(),
                lastLoginAt == null ? null : lastLoginAt.toDate().toInstant(),
                Boolean.TRUE.equals(snapshot.getBoolean("kakkdugi")),
                snapshot.getString("pledgeId")
        );
    }
}
