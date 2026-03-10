package org.example.owoonwan.title.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.example.owoonwan.title.domain.TitleRules;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnBean(Firestore.class)
@RequiredArgsConstructor
public class FirestoreTitleRuleRepository implements TitleRuleRepository {

    private final Firestore firestore;

    @Override
    public Optional<TitleRules> findRules() {
        DocumentSnapshot snapshot = FirestoreAwait.get(
                firestore.collection("settings").document("titlesRules").get()
        );
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
