package com.smartfactory;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Order类的单元测试
 * 优化：使用相对时间，封装构造过程，重点验证"对齐到当天8点"的业务逻辑
 */
public class OrderTest {

    // 设定一个基准日期（例如明天），用于构建测试场景
    private final LocalDate targetDate = LocalDate.now().plusDays(1);

    // 预期的对齐时间：基准日期的早上 8:00
    private final LocalDateTime expectedAlignedTime = LocalDateTime.of(targetDate, LocalTime.of(8, 0));

    /**
     * 辅助方法：快速创建一个仅关注截止时间的订单
     * 其他字段使用默认哑元数据(Dummy Data)
     */
    private Order createOrderWithDeadline(LocalDateTime deadline) {
        // 默认到达时间设置为截止时间的前一天
        LocalDateTime arrivalTime = deadline.minusDays(1);
        return new Order(1, 1, 10, 1000.0, deadline, arrivalTime);
    }

    @Test
    public void testGetAlignedDeadline_Morning8AM() {
        // 测试：截止时间正好是早上8点
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(8, 0));
        Order order = createOrderWithDeadline(deadline);

        assertEquals("8点整应该保持不变", expectedAlignedTime, order.getAlignedDeadline());
    }

    @Test
    public void testGetAlignedDeadline_Afternoon() {
        // 测试：截止时间是下午 14:30 -> 应对齐到 08:00
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(14, 30));
        Order order = createOrderWithDeadline(deadline);

        assertEquals("下午时间应对齐到当天早上8点", expectedAlignedTime, order.getAlignedDeadline());
    }

    @Test
    public void testGetAlignedDeadline_Night() {
        // 测试：截止时间是晚上 23:59 -> 应对齐到 08:00
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(23, 59));
        Order order = createOrderWithDeadline(deadline);

        assertEquals("晚上时间应对齐到当天早上8点", expectedAlignedTime, order.getAlignedDeadline());
    }

    @Test
    public void testGetAlignedDeadline_EarlyMorning() {
        // 测试：截止时间是凌晨 03:00 -> 应对齐到 08:00
        // 注意：根据原有业务逻辑，当天的凌晨也会被"推迟/对齐"到当天的8点
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(3, 0));
        Order order = createOrderWithDeadline(deadline);

        assertEquals("凌晨时间应对齐到当天早上8点", expectedAlignedTime, order.getAlignedDeadline());
    }

    @Test
    public void testGetAlignedDeadline_Exactly8AM_Detailed() {
        // 测试：秒数归零的情况
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(8, 0, 0));
        Order order = createOrderWithDeadline(deadline);

        LocalDateTime result = order.getAlignedDeadline();
        assertEquals(expectedAlignedTime, result);
        assertEquals("秒数应该为0", 0, result.getSecond());
        assertEquals("纳秒应该为0", 0, result.getNano());
    }

    @Test
    public void testGetAlignedDeadline_Before8AM() {
        // 测试：早上 07:59 -> 应对齐到 08:00
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(7, 59));
        Order order = createOrderWithDeadline(deadline);

        assertEquals("8点前的时间应对齐到当天早上8点", expectedAlignedTime, order.getAlignedDeadline());
    }

    @Test
    public void testGetters() {
        // 测试：基础Getter方法
        LocalDateTime deadline = LocalDateTime.of(targetDate, LocalTime.of(14, 0));
        LocalDateTime arrival = LocalDateTime.of(targetDate.minusDays(1), LocalTime.of(8, 0));

        Order order = new Order(99, 2, 5, 500.0, deadline, arrival);

        assertEquals(99, order.getId());
        assertEquals(2, order.getProductId());
        assertEquals(5, order.getQuantity());
        assertEquals(500.0, order.getTotalValue(), 0.001);
        assertEquals(deadline, order.getDeadline());
        assertEquals(arrival, order.getArrivalTime());
    }

    /**
     * 补充测试：跨天情况（验证日期并未改变，只改变了时间）
     */
    @Test
    public void testAlignmentPreservesDate() {
        LocalDate anotherDate = targetDate.plusDays(5); // 5天后
        LocalDateTime deadline = LocalDateTime.of(anotherDate, LocalTime.of(15, 0));
        Order order = createOrderWithDeadline(deadline);

        LocalDateTime result = order.getAlignedDeadline();

        assertEquals("日期部分应该保持不变", anotherDate, result.toLocalDate());
        assertEquals("时间部分应对齐到8点", LocalTime.of(8, 0), result.toLocalTime());
    }
}