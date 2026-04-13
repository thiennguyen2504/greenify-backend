package com.webdev.greenify.common.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class TimeUtils {

    public boolean isValidTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return startTime.isBefore(endTime);
    }
}
