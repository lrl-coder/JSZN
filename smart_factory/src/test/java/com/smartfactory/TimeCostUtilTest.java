package com.smartfactory;

import com.smartfactory.util.TimeCostUtil;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;

/**
 * TimeCostUtil类的单元测试
 * 测试不同时间段的工资成本系数
 */
public class TimeCostUtilTest {

    @Test
    public void testGetCostCoefficient_NormalShift() {
        // 测试：正常班 (8:00 - 16:00)
        LocalDateTime time1 = LocalDateTime.of(2025, 11, 26, 8, 0);
        assertEquals(1.0, TimeCostUtil.getCostCoefficient(time1), 0.001);

        LocalDateTime time2 = LocalDateTime.of(2025, 11, 26, 12, 0);
        assertEquals(1.0, TimeCostUtil.getCostCoefficient(time2), 0.001);

        LocalDateTime time3 = LocalDateTime.of(2025, 11, 26, 15, 59);
        assertEquals(1.0, TimeCostUtil.getCostCoefficient(time3), 0.001);
    }

    @Test
    public void testGetCostCoefficient_EveningShift() {
        // 测试：晚班 (16:00 - 20:00)
        LocalDateTime time1 = LocalDateTime.of(2025, 11, 26, 16, 0);
        assertEquals(1.2, TimeCostUtil.getCostCoefficient(time1), 0.001);

        LocalDateTime time2 = LocalDateTime.of(2025, 11, 26, 18, 0);
        assertEquals(1.2, TimeCostUtil.getCostCoefficient(time2), 0.001);

        LocalDateTime time3 = LocalDateTime.of(2025, 11, 26, 19, 59);
        assertEquals(1.2, TimeCostUtil.getCostCoefficient(time3), 0.001);
    }

    @Test
    public void testGetCostCoefficient_NightShift() {
        // 测试：夜班 (20:00 - 0:00)
        LocalDateTime time1 = LocalDateTime.of(2025, 11, 26, 20, 0);
        assertEquals(1.5, TimeCostUtil.getCostCoefficient(time1), 0.001);

        LocalDateTime time2 = LocalDateTime.of(2025, 11, 26, 22, 0);
        assertEquals(1.5, TimeCostUtil.getCostCoefficient(time2), 0.001);

        LocalDateTime time3 = LocalDateTime.of(2025, 11, 26, 23, 59);
        assertEquals(1.5, TimeCostUtil.getCostCoefficient(time3), 0.001);

        LocalDateTime time4 = LocalDateTime.of(2025, 11, 27, 0, 0);
        assertEquals(1.5, TimeCostUtil.getCostCoefficient(time4), 0.001);
    }

    @Test
    public void testGetCostCoefficient_LateNightShift() {
        // 测试：深夜班 (0:00 - 8:00)
        LocalDateTime time1 = LocalDateTime.of(2025, 11, 27, 0, 1);
        assertEquals(2.0, TimeCostUtil.getCostCoefficient(time1), 0.001);

        LocalDateTime time2 = LocalDateTime.of(2025, 11, 27, 4, 0);
        assertEquals(2.0, TimeCostUtil.getCostCoefficient(time2), 0.001);

        LocalDateTime time3 = LocalDateTime.of(2025, 11, 27, 7, 59);
        assertEquals(2.0, TimeCostUtil.getCostCoefficient(time3), 0.001);
    }

    @Test
    public void testGetCostCoefficient_BoundaryCases() {
        // 测试：边界情况
        // 7:59 - 深夜班（8点之前都算深夜班）
        LocalDateTime time1 = LocalDateTime.of(2025, 11, 26, 7, 59);
        assertEquals(2.0, TimeCostUtil.getCostCoefficient(time1), 0.001);

        // 8:00 - 正常班
        LocalDateTime time2 = LocalDateTime.of(2025, 11, 26, 8, 0);
        assertEquals(1.0, TimeCostUtil.getCostCoefficient(time2), 0.001);

        // 15:59 - 正常班
        LocalDateTime time3 = LocalDateTime.of(2025, 11, 26, 15, 59);
        assertEquals(1.0, TimeCostUtil.getCostCoefficient(time3), 0.001);

        // 16:00 - 晚班
        LocalDateTime time4 = LocalDateTime.of(2025, 11, 26, 16, 0);
        assertEquals(1.2, TimeCostUtil.getCostCoefficient(time4), 0.001);
    }

    @Test
    public void testBasePayConstant() {
        // 测试：基础工资常量
        assertEquals(200.0, TimeCostUtil.BASE_PAY_4_HOURS, 0.001);
    }
}

