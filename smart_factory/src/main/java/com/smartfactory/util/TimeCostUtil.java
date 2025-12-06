package com.smartfactory.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeCostUtil {
    // 假设 BasePay 为每 4 小时 (一个加工块) 的基础工资
    public static final double BASE_PAY_4_HOURS = 200.0;

    /**
     * 根据任务的开始时间计算其所在的 4 小时时间块的工资成本系数。
     */
    public static double getCostCoefficient(LocalDateTime startTime) {
        LocalTime time = startTime.toLocalTime();
        int hour = time.getHour();

        // 0:00 (MIDNIGHT) - 夜班，系数1.5
        if (time.equals(LocalTime.MIDNIGHT)) {
            return 1.5;
        }
        // 0:01 - 7:59 (深夜班) - 系数2.0，8点之前都算深夜班
        else if (hour >= 0 && hour < 8) {
            return 2.0;
        }
        // 8:00 - 15:59 (正常班) - 系数1.0
        else if (hour >= 8 && hour < 16) {
            return 1.0;
        }
        // 16:00 - 19:59 (晚班) - 系数1.2
        else if (hour >= 16 && hour < 20) {
            return 1.2;
        }
        // 20:00 - 23:59 (夜班) - 系数1.5
        else if (hour >= 20) {
            return 1.5;
        }

        // 默认值 (应避免发生)
        return 1.0;
    }
}
