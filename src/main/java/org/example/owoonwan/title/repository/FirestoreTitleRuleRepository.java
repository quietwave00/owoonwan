package org.example.owoonwan.title.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.firebase.FirestoreClientProvider;
import org.example.owoonwan.title.domain.TitleRules;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnBean(FirestoreClientProvider.class)
@RequiredArgsConstructor
public class FirestoreTitleRuleRepository implements TitleRuleRepository {

    private final FirestoreClientProvider firestoreClientProvider;

    @Override
    public Optional<TitleRules> findRules() {
        DocumentSnapshot snapshot = firestoreClientProvider.execute("find title rules",
                firestore -> firestore.collection("settings").document("titlesRules").get());
        if (!snapshot.exists()) {
            return Optional.empty();
        }

        Long weeklyHumanThreshold = snapshot.getLong("weeklyHumanThreshold");
        Long monthlyHumanThreshold = snapshot.getLong("monthlyHumanThreshold");

        return Optional.of(new TitleRules(
                weeklyHumanThreshold == null ? 3 : weeklyHumanThreshold.intValue(),
                monthlyHumanThreshold == null ? 12 : monthlyHumanThreshold.intValue()
        ));
    }
}
