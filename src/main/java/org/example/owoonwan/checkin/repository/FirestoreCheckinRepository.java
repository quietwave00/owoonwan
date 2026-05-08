package org.example.owoonwan.checkin.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FirestoreCheckinRepository implements CheckinRepository {

    private final FirestoreClientProvider firestoreClientProvider;

    @Override
    public Checkin save(CheckinSaveCommand command) {
        firestoreClientProvider.execute("save checkin",
                firestore -> firestore.collection("checkins").document(command.documentId()).set(toPayload(command)));
        return toCheckin(command);
    }

    @Override
    public List<Checkin> saveAll(List<CheckinSaveCommand> commands) {
        firestoreClientProvider.execute("save all checkins", firestore -> {
            WriteBatch batch = firestore.batch();
            for (CheckinSaveCommand command : commands) {
                DocumentReference checkinRef = firestore.collection("checkins").document(command.documentId());
                batch.set(checkinRef, toPayload(command));
            }
            return batch.commit();
        });
        return commands.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByDate(String date) {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find checkins by date",
                firestore -> firestore.collection("checkins")
                        .whereEqualTo("date", date)
                        .orderBy("userId")
                        .get()).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByUserIdAndDateRange(String userId, String startDate, String endDate) {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find checkins by date range",
                firestore -> firestore.collection("checkins")
                        .whereEqualTo("userId", userId)
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                        .orderBy("date")
                        .get()).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey) {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find checkins by month key",
                firestore -> firestore.collection("checkins")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("monthKey", monthKey)
                        .orderBy("date")
                        .get()).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByWeekKey(String weekKey) {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find checkins by week key",
                firestore -> firestore.collection("checkins")
                        .whereEqualTo("weekKey", weekKey)
                        .orderBy("userId")
                        .orderBy("date")
                        .get()).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    @Override
    public List<Checkin> findByMonthKey(String monthKey) {
        List<QueryDocumentSnapshot> documents = firestoreClientProvider.execute("find checkins by month key",
                firestore -> firestore.collection("checkins")
                        .whereEqualTo("monthKey", monthKey)
                        .orderBy("userId")
                        .orderBy("date")
                        .get()).getDocuments();
        return documents.stream().map(this::toCheckin).toList();
    }

    private Map<String, Object> toPayload(CheckinSaveCommand command) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", command.userId());
        payload.put("date", command.date());
        payload.put("weekKey", command.weekKey());
        payload.put("monthKey", command.monthKey());
        payload.put("status", command.status().name());
        payload.put("checkedAt", Date.from(command.checkedAt()));
        return payload;
    }

    private Checkin toCheckin(CheckinSaveCommand command) {
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
