package org.example.owoonwan.checkin.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FirestoreCheckinRepository implements CheckinRepository {

    private final Firestore firestore;

    @Override
    public Checkin save(CheckinSaveCommand command) {
        DocumentReference checkinRef = checkins().document(command.documentId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", command.userId());
        payload.put("date", command.date());
        payload.put("weekKey", command.weekKey());
        payload.put("monthKey", command.monthKey());
        payload.put("status", command.status().name());
        payload.put("checkedAt", Date.from(command.checkedAt()));
        FirestoreAwait.get(checkinRef.set(payload));
        return new Checkin(
                command.documentId(),
                command.userId(),
                command.date(),
                command.weekKey(),
                command.monthKey(),
                command.status(),
                command.checkedAt()
        );
    }

    @Override
    public List<Checkin> findByUserIdAndDateRange(String userId, String startDate, String endDate) {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                checkins()
                        .whereEqualTo("userId", userId)
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                        .orderBy("date")
                        .get()
        ).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey) {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                checkins()
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("monthKey", monthKey)
                        .orderBy("date")
                        .get()
        ).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByWeekKey(String weekKey) {
        List<QueryDocumentSnapshot> documents = FirestoreAwait.get(
                checkins()
                        .whereEqualTo("weekKey", weekKey)
                        .orderBy("userId")
                        .orderBy("date")
                        .get()
        ).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    private CollectionReference checkins() {
        return firestore.collection("checkins");
    }

    private Checkin toCheckin(DocumentSnapshot snapshot) {
        Timestamp checkedAt = snapshot.getTimestamp("checkedAt");
        return new Checkin(
                snapshot.getId(),
                snapshot.getString("userId"),
                snapshot.getString("date"),
                snapshot.getString("weekKey"),
                snapshot.getString("monthKey"),
                CheckinStatus.valueOf(snapshot.getString("status")),
                checkedAt == null ? null : checkedAt.toDate().toInstant()
        );
    }
}
