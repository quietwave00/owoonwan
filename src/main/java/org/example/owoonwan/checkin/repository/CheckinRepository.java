package org.example.owoonwan.checkin.repository;

import org.example.owoonwan.checkin.domain.Checkin;

import java.util.List;

public interface CheckinRepository {

    Checkin save(CheckinSaveCommand command);

    List<Checkin> findByDate(String date);

    List<Checkin> findByUserIdAndDateRange(String userId, String startDate, String endDate);

    List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey);

    List<Checkin> findByWeekKey(String weekKey);
}
