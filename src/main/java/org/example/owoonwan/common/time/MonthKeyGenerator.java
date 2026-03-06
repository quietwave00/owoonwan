package org.example.owoonwan.common.time;

import org.example.owoonwan.time.TimeKeyUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MonthKeyGenerator {

    public String generate(Instant instant) {
        return TimeKeyUtil.deriveMonthKey(instant);
    }
}
