package com.smartfactory;

import java.time.LocalDateTime;

public class TestTool {
    public static LocalDateTime getPlanStartTime() {LocalDateTime now = LocalDateTime.now();
        LocalDateTime today8AM = now.toLocalDate().atTime(8, 0);
        LocalDateTime planStartTime = today8AM;
        return planStartTime;
    }
}
