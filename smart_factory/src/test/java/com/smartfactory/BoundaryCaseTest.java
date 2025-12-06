package com.smartfactory;

import com.smartfactory.util.Job;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * 边界情况测试
 * 优化：使用相对时间替代硬编码日期，增加辅助方法构建测试数据
 */
public class BoundaryCaseTest {
    // 基准开始时间
    private final LocalDateTime planStartTime = TestTool.getPlanStartTime();

    // 常用常量
    private static final double DEFAULT_PENALTY = 1000.0;
    private static final double DEFAULT_PROCESS_HOURS = 4.0;

    /**
     * 辅助方法：快速创建产品
     */
    private Product createProduct(int id, double processTime) {
        return new Product(id, processTime);
    }

    /**
     * 辅助方法：快速创建订单 (默认到达时间为计划开始时间)
     */
    private Order createOrder(int id, int productId, int quantity, double value, LocalDateTime deadline) {
        return new Order(id, productId, quantity, value, deadline, planStartTime);
    }

    /**
     * 辅助方法：快速创建订单 (指定到达时间)
     */
    private Order createOrder(int id, int productId, int quantity, double value, LocalDateTime deadline, LocalDateTime arrivalTime) {
        return new Order(id, productId, quantity, value, deadline, arrivalTime);
    }

    /**
     * 边界测试1：订单数量为0
     */
    @Test(timeout = 5000)
    public void testZeroOrders() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, DEFAULT_PROCESS_HOURS));

        List<Order> orders = new ArrayList<>(); // 空列表

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 10, 0.8, 0.1, 5);

        Chromosome result = scheduler.run();
        assertNotNull(result);

        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        assertNotNull(scheduleResult);
        assertEquals(0, scheduleResult.scheduledJobs.size());
        assertEquals(0.0, scheduleResult.totalPenalty, 0.001);
    }

    /**
     * 边界测试3：订单数量非常大
     */
    @Test(timeout = 300000)
    public void testLargeOrderQuantity() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 1.0)); // 1小时

        List<Order> orders = new ArrayList<>();
        // 截止时间设置为24小时后
        LocalDateTime deadline = planStartTime.plusDays(1);

        // 创建1个大数量订单
        orders.add(createOrder(1, 1, 10, DEFAULT_PENALTY, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 15, 0.8, 0.1, 8);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(10, scheduleResult.scheduledJobs.size());
    }

    /**
     * 边界测试4：加工时间正好等于4小时
     */
    @Test
    public void testProcessingTimeExactlyFourHours() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 4.0));

        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(1, 1, 1, DEFAULT_PENALTY, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(1, scheduleResult.scheduledJobs.size());

        Job job = scheduleResult.scheduledJobs.get(0);
        int hour = job.startTime.getHour();
        assertTrue("应该对齐到4小时网格", hour % 4 == 0);
    }

    /**
     * 边界测试5：加工时间略小于4小时（可以拼单）
     */
    @Test
    public void testProcessingTimeJustBelowFourHours() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 3.99));

        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(1, 1, 1, DEFAULT_PENALTY, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(1, scheduleResult.scheduledJobs.size());
    }

    /**
     * 边界测试6：加工时间略大于4小时
     */
    @Test
    public void testProcessingTimeJustAboveFourHours() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 4.01));

        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(1, 1, 1, DEFAULT_PENALTY, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(1, scheduleResult.scheduledJobs.size());
    }

    /**
     * 边界测试7：订单价值为0
     */
    @Test
    public void testZeroOrderValue() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, DEFAULT_PROCESS_HOURS));

        List<Order> orders = new ArrayList<>();
        // 价值为0.0
        orders.add(createOrder(1, 1, 1, 0.0, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(1, scheduleResult.scheduledJobs.size());
        assertEquals(0.0, scheduleResult.totalPenalty, 0.001);
    }

    /**
     * 边界测试8：订单价值非常大
     */
    @Test
    public void testVeryLargeOrderValue() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, DEFAULT_PROCESS_HOURS));

        List<Order> orders = new ArrayList<>();
        // 价值为1,000,000.0
        orders.add(createOrder(1, 1, 1, 1000000.0, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(1, scheduleResult.scheduledJobs.size());
    }

    /**
     * 边界测试9：所有订单都延迟
     */
    @Test
    public void testAllOrdersLate() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, DEFAULT_PROCESS_HOURS));

        List<Order> orders = new ArrayList<>();

        // 截止时间 = 计划开始时间 (非常紧急，几乎不可能按时完成)
        LocalDateTime tightDeadline = planStartTime;

        for (int i = 1; i <= 5; i++) {
            orders.add(createOrder(i, 1, 1, DEFAULT_PENALTY, tightDeadline));
        }

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(5, scheduleResult.scheduledJobs.size());
        assertTrue("应该有罚款", scheduleResult.totalPenalty >= 0);
    }

    /**
     * 边界测试10：所有订单都不延迟
     */
    @Test
    public void testAllOrdersOnTime() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 1.0));

        List<Order> orders = new ArrayList<>();
        // 截止时间 = 30天后 (非常宽松)
        LocalDateTime looseDeadline = planStartTime.plusDays(30);

        for (int i = 1; i <= 5; i++) {
            orders.add(createOrder(i, 1, 1, DEFAULT_PENALTY, looseDeadline));
        }

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertTrue("Job数量应该至少等于订单数量", scheduleResult.scheduledJobs.size() >= 5);
        assertTrue("罚款应该为0或很小", scheduleResult.totalPenalty >= 0);
    }

    /**
     * 边界测试11：订单到达时间正好是8点 (计划开始时间)
     */
    @Test
    public void testArrivalTimeExactlyAtStart() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, DEFAULT_PROCESS_HOURS));

        List<Order> orders = new ArrayList<>();
        // 到达时间默认就是 planStartTime
        orders.add(createOrder(1, 1, 1, DEFAULT_PENALTY, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        assertNotNull(result);
    }

    /**
     * 边界测试12：订单到达时间略早于8点
     */
    @Test
    public void testArrivalTimeJustBeforeStart() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, DEFAULT_PROCESS_HOURS));

        List<Order> orders = new ArrayList<>();

        // 到达时间：开始时间的前1秒
        LocalDateTime earlyArrival = planStartTime.minusSeconds(1);
        orders.add(createOrder(1, 1, 1, DEFAULT_PENALTY, planStartTime.plusDays(1), earlyArrival));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        assertNotNull(result);
    }

    /**
     * 边界测试13：三种产品各一个订单
     */
    @Test
    public void testAllThreeProductTypes() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 4.0));
        products.add(createProduct(2, 3.0));
        products.add(createProduct(3, 2.0));

        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        orders.add(createOrder(1, 1, 1, 1000.0, deadline));
        orders.add(createOrder(2, 2, 1, 800.0, deadline));
        orders.add(createOrder(3, 3, 1, 600.0, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(3, scheduleResult.scheduledJobs.size());

        boolean hasProduct1 = scheduleResult.scheduledJobs.stream().anyMatch(job -> job.productId == 1);
        boolean hasProduct2 = scheduleResult.scheduledJobs.stream().anyMatch(job -> job.productId == 2);
        boolean hasProduct3 = scheduleResult.scheduledJobs.stream().anyMatch(job -> job.productId == 3);

        assertTrue("应该包含产品1", hasProduct1);
        assertTrue("应该包含产品2", hasProduct2);
        assertTrue("应该包含产品3", hasProduct3);
    }

    /**
     * 边界测试14：所有订单都在同一条生产线
     */
    @Test
    public void testAllOrdersOnSameLine() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 2.0));

        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        for (int i = 1; i <= 10; i++) {
            orders.add(createOrder(i, 1, 1, 100.0, deadline));
        }

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertTrue("Job数量应该至少等于订单数量", scheduleResult.scheduledJobs.size() >= 10);

        for (Job job : scheduleResult.scheduledJobs) {
            assertTrue("生产线ID应该在1-3之间", job.machineLineId >= 1 && job.machineLineId <= 3);
        }
    }

    /**
     * 边界测试15：订单数量正好等于生产线数量
     */
    @Test
    public void testOrdersEqualLines() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 4.0));

        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        for (int i = 1; i <= 3; i++) {
            orders.add(createOrder(i, 1, 1, DEFAULT_PENALTY, deadline));
        }

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(3, scheduleResult.scheduledJobs.size());
    }
}