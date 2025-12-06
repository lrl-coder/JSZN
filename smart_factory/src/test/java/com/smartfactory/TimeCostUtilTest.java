package com.smartfactory;

import com.smartfactory.util.TimeCostUtil;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TimeCostUtil类的单元测试
 * 优化：使用相对时间，提取常量，验证不同时间段的工资成本系数
 */
public class TimeCostUtilTest {

    // 基准日期（日期本身不影响系数，只看时分）
    private final LocalDate testDate = LocalDate.now();

    // 预期系数常量，提高可读性
    private static final double COEFF_NORMAL = 1.0;      // 08:00 - 16:00
    private static final double COEFF_EVENING = 1.2;     // 16:00 - 20:00
    private static final double COEFF_NIGHT = 1.5;       // 20:00 - 00:00
    private static final double COEFF_LATE_NIGHT = 2.0;  // 00:00 - 08:00

    /**
     * 辅助方法：快速构建当天的测试时间
     */
    private LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(testDate, LocalTime.of(hour, minute));
    }

    /**
     * 辅助方法：快速构建次日的测试时间 (用于测试跨天边界，如00:00)
     */
    private LocalDateTime nextDayAt(int hour, int minute) {
        return LocalDateTime.of(testDate.plusDays(1), LocalTime.of(hour, minute));
    }

    @Test
    public void testGetCostCoefficient_NormalShift() {
        // 正常班 (8:00 - 16:00)
        assertEquals("08:00 应为正常班", COEFF_NORMAL, TimeCostUtil.getCostCoefficient(at(8, 0)), 0.001);
        assertEquals("12:00 应为正常班", COEFF_NORMAL, TimeCostUtil.getCostCoefficient(at(12, 0)), 0.001);
        assertEquals("15:59 应为正常班", COEFF_NORMAL, TimeCostUtil.getCostCoefficient(at(15, 59)), 0.001);
    }

    @Test
    public void testGetCostCoefficient_EveningShift() {
        // 晚班 (16:00 - 20:00)
        assertEquals("16:00 应开始晚班", COEFF_EVENING, TimeCostUtil.getCostCoefficient(at(16, 0)), 0.001);
        assertEquals("18:00 应为晚班", COEFF_EVENING, TimeCostUtil.getCostCoefficient(at(18, 0)), 0.001);
        assertEquals("19:59 应为晚班", COEFF_EVENING, TimeCostUtil.getCostCoefficient(at(19, 59)), 0.001);
    }

    @Test
    public void testGetCostCoefficient_NightShift() {
        // 夜班 (20:00 - 00:00)
        assertEquals("20:00 应开始夜班", COEFF_NIGHT, TimeCostUtil.getCostCoefficient(at(20, 0)), 0.001);
        assertEquals("22:00 应为夜班", COEFF_NIGHT, TimeCostUtil.getCostCoefficient(at(22, 0)), 0.001);
        assertEquals("23:59 应为夜班", COEFF_NIGHT, TimeCostUtil.getCostCoefficient(at(23, 59)), 0.001);

        // 边界：正好是第二天的 00:00，通常视作上一班次的结束点或通过特定逻辑处理
        assertEquals("次日00:00 仍算作前一班次(夜班)结束", COEFF_NIGHT, TimeCostUtil.getCostCoefficient(nextDayAt(0, 0)), 0.001);
    }

    @Test
    public void testGetCostCoefficient_LateNightShift() {
        // 深夜班 (00:00 - 08:00)
        // 注意：这里使用次日时间，模拟跨夜后的凌晨
        assertEquals("00:01 应为深夜班", COEFF_LATE_NIGHT, TimeCostUtil.getCostCoefficient(nextDayAt(0, 1)), 0.001);
        assertEquals("04:00 应为深夜班", COEFF_LATE_NIGHT, TimeCostUtil.getCostCoefficient(nextDayAt(4, 0)), 0.001);
        assertEquals("07:59 应为深夜班", COEFF_LATE_NIGHT, TimeCostUtil.getCostCoefficient(nextDayAt(7, 59)), 0.001);
    }

    @Test
    public void testGetCostCoefficient_BoundaryCases() {
        // 重点测试临界点跳变

        // 07:59 vs 08:00 (深夜班 -> 正常班)
        assertEquals("07:59 是深夜班", COEFF_LATE_NIGHT, TimeCostUtil.getCostCoefficient(at(7, 59)), 0.001);
        assertEquals("08:00 是正常班", COEFF_NORMAL, TimeCostUtil.getCostCoefficient(at(8, 0)), 0.001);

        // 15:59 vs 16:00 (正常班 -> 晚班)
        assertEquals("15:59 是正常班", COEFF_NORMAL, TimeCostUtil.getCostCoefficient(at(15, 59)), 0.001);
        assertEquals("16:00 是晚班", COEFF_EVENING, TimeCostUtil.getCostCoefficient(at(16, 0)), 0.001);

        // 19:59 vs 20:00 (晚班 -> 夜班)
        assertEquals("19:59 是晚班", COEFF_EVENING, TimeCostUtil.getCostCoefficient(at(19, 59)), 0.001);
        assertEquals("20:00 是夜班", COEFF_NIGHT, TimeCostUtil.getCostCoefficient(at(20, 0)), 0.001);
    }

    @Test
    public void testBasePayConstant() {
        // 测试基础工资常量
        assertEquals("基础4小时工资应为200.0", 200.0, TimeCostUtil.BASE_PAY_4_HOURS, 0.001);
    }
}