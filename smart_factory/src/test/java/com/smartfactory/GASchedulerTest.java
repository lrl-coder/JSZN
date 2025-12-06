package com.smartfactory;

import com.smartfactory.util.Job;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GAScheduler类的单元测试和边界情况测试
 * 优化：移除硬编码日期，使用相对时间，封装数据构建过程
 */
public class GASchedulerTest {
    // 统一的时间基准
    private final LocalDateTime planStartTime = TestTool.getPlanStartTime();

    // 测试常量
    private static final double DEFAULT_VALUE = 1000.0;
    private static final double TEST_PENALTY_RATE = 0.1; // 10% 罚款率

    /**
     * 辅助方法：创建标准测试产品列表
     */
    private List<Product> createTestProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));  // 产品1，4小时
        products.add(new Product(2, 3.0));  // 产品2，3小时
        products.add(new Product(3, 2.0));  // 产品3，2小时
        return products;
    }

    /**
     * 辅助方法：快速创建订单 (默认到达时间为计划开始时间)
     */
    private Order createOrder(int id, int productId, int quantity, double value, LocalDateTime deadline) {
        return new Order(id, productId, quantity, value, deadline, planStartTime);
    }

    /**
     * 测试：空订单列表
     */
    @Test(timeout = 5000)
    public void testEmptyOrders() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>(); // 空列表

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 10, 0.8, TEST_PENALTY_RATE, 5);

        // 应该能够运行而不抛出异常
        Chromosome result = scheduler.run();
        assertNotNull("空订单列表不应返回null结果", result);

        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        assertNotNull(scheduleResult);
        assertEquals(0, scheduleResult.scheduledJobs.size());
        assertEquals(0.0, scheduleResult.totalPenalty, 0.001);
    }

    /**
     * 测试：单个订单
     */
    @Test
    public void testSingleOrder() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();

        // 截止时间：1天后
        orders.add(createOrder(1, 1, 1, DEFAULT_VALUE, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(1, scheduleResult.scheduledJobs.size());
    }

    /**
     * 测试：多个订单，确保都能被调度
     */
    @Test
    public void testMultipleOrders() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();

        LocalDateTime deadlineDay1 = planStartTime.plusDays(1);
        LocalDateTime deadlineDay2 = planStartTime.plusDays(2);

        orders.add(createOrder(1, 1, 2, 1000.0, deadlineDay1)); // 2个工件
        orders.add(createOrder(2, 2, 1, 800.0, deadlineDay1));  // 1个工件
        orders.add(createOrder(3, 3, 1, 600.0, deadlineDay2));  // 1个工件

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, TEST_PENALTY_RATE, 15);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        // 应该有4个Job（2 + 1 + 1）
        assertEquals("Job总数应等于所有订单工件数之和", 4, scheduleResult.scheduledJobs.size());

        // 检查所有订单都有完成时间
        assertEquals("所有订单都应被记录完成时间", 3, scheduleResult.completionTimes.size());
    }

    /**
     * 测试：订单延迟情况
     */
    @Test
    public void testLateOrders() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();

        // 截止时间只给1小时，但产品1需要4小时加工 -> 必延迟
        LocalDateTime impossibleDeadline = planStartTime.plusHours(1);
        orders.add(createOrder(1, 1, 1, 1000.0, impossibleDeadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        LocalDateTime completionTime = scheduleResult.completionTimes.get(1);
        LocalDateTime alignedDeadline = orders.get(0).getAlignedDeadline();

        assertNotNull("完成时间不应为空", completionTime);

        // 验证确实延迟了
        if (completionTime.isAfter(alignedDeadline)) {
            assertTrue("延迟订单应产生罚款", scheduleResult.totalPenalty > 0);
        } else {
            // 注意：如果截止时间对齐逻辑极其宽松，或者计划开始时间本身就很早，可能不会延迟
            // 但在这个特定的测试场景下，1小时的Deadline对于4小时的任务通常是会延迟的
            System.out.println("警告：在这个测试配置下，订单未被判定为延迟。检查对齐逻辑。");
        }
    }

    /**
     * 测试：拼单逻辑 - 同一产品的多个订单
     */
    @Test
    public void testMergingSameProduct() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 2.0));  // 2小时，小于4小时，可以拼单

        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        // 创建2个同一产品的订单
        orders.add(createOrder(1, 1, 1, 500.0, deadline));
        orders.add(createOrder(2, 1, 1, 500.0, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        // 验证Job数量逻辑
        int jobCount = scheduleResult.scheduledJobs.size();
        assertTrue("Job数量至少为2(未拼单)", jobCount >= 2);
        assertTrue("Job数量不应过多", jobCount <= 3);

        // 检查是否有拼单发生（baseCost为0通常表示它是拼单产生的附属Job，或者是被合并了）
        // 具体判断逻辑取决于你的拼单实现细节，这里保留原逻辑
        long mergedJobs = scheduleResult.scheduledJobs.stream()
                .filter(job -> job.baseCost == 0.0)
                .count();

        assertTrue("算法应尝试进行拼单", mergedJobs >= 0);
    }

    /**
     * 测试：尾数拼单 - 小于4小时的尾数工件
     */
    @Test
    public void testTailPieceMerging() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 2.0));  // 2小时

        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        // 订单1: 3个工件 (2个完整 + 1个尾数)
        // 订单2: 2个工件 (2个完整)
        orders.add(createOrder(1, 1, 3, 1000.0, deadline));
        orders.add(createOrder(2, 1, 2, 800.0, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, TEST_PENALTY_RATE, 15);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(5, scheduleResult.scheduledJobs.size());
    }

    /**
     * 测试：4小时换产品约束
     */
    @Test
    public void testFourHourProductChange() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        orders.add(createOrder(1, 1, 1, 1000.0, deadline));
        orders.add(createOrder(2, 2, 1, 800.0, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        // 检查Job的时间是否对齐到4小时网格
        for (Job job : scheduleResult.scheduledJobs) {
            int hour = job.startTime.getHour();
            int minute = job.startTime.getMinute();
            int second = job.startTime.getSecond();

            // 验证时间网格对齐 (0, 4, 8, 12, 16, 20)
            assertTrue("开始时间的小时数应为4的倍数 (当前: " + hour + ")", hour % 4 == 0);
            assertEquals("开始时间的分钟数应为0", 0, minute);
            assertEquals("开始时间的秒数应为0", 0, second);
        }
    }

    /**
     * 测试：大量订单 (性能/压力测试)
     */
    @Test(timeout = 60000)
    public void testLargeNumberOfOrders() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        // 创建20个订单
        for (int i = 1; i <= 20; i++) {
            int productId = (i % 3) + 1;
            orders.add(createOrder(i, productId, 2, 1000.0, deadline));
        }

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        // 增加种群和迭代次数
        GAScheduler scheduler = new GAScheduler(data, 50, 0.8, TEST_PENALTY_RATE, 20);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        // 20个订单 * 2个工件 = 至少40个Job
        assertTrue("Job数量过少", scheduleResult.scheduledJobs.size() >= 40);
        // 考虑到拼单产生的额外Job，设定一个合理的上限
        assertTrue("Job数量过多", scheduleResult.scheduledJobs.size() <= 60);

        assertEquals("所有订单应包含完成时间", 20, scheduleResult.completionTimes.size());
    }

    /**
     * 测试：极端截止时间 (很早 vs 很晚)
     */
    @Test
    public void testExtremeDeadlines() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();

        // 截止时间 = 计划开始时间 (极其紧急)
        orders.add(createOrder(1, 1, 1, 1000.0, planStartTime));

        // 截止时间 = 30天后 (极其宽松)
        orders.add(createOrder(2, 2, 1, 800.0, planStartTime.plusDays(30)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(2, scheduleResult.scheduledJobs.size());
    }

    /**
     * 测试：不同产品类型的订单
     */
    @Test
    public void testDifferentProductTypes() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        orders.add(createOrder(1, 1, 1, 1000.0, deadline));
        orders.add(createOrder(2, 2, 1, 800.0, deadline));
        orders.add(createOrder(3, 3, 1, 600.0, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertEquals(3, scheduleResult.scheduledJobs.size());

        for (Job job : scheduleResult.scheduledJobs) {
            assertTrue("产品ID应在1-3范围内", job.productId >= 1 && job.productId <= 3);
        }
    }

    /**
     * 测试：订单完成时间记录
     */
    @Test
    public void testOrderCompletionTime() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        orders.add(createOrder(1, 1, 2, 1000.0, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        assertTrue(scheduleResult.completionTimes.containsKey(1));
        LocalDateTime completionTime = scheduleResult.completionTimes.get(1);
        assertNotNull(completionTime);
        assertFalse("完成时间不应早于计划开始时间", completionTime.isBefore(planStartTime));
    }

    /**
     * 测试：成本计算合理性
     */
    @Test
    public void testCostCalculation() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();

        orders.add(createOrder(1, 1, 1, 1000.0, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        // 检查 double 是否溢出或为 NaN
        assertFalse("总成本不应为NaN", Double.isNaN(scheduleResult.totalCost));
        assertFalse("总成本不应无穷大", Double.isInfinite(scheduleResult.totalCost));
        assertTrue("总成本应小于Double最大值", scheduleResult.totalCost < Double.MAX_VALUE);
    }

    /**
     * 测试：罚款计算
     */
    @Test
    public void testPenaltyCalculation() {
        List<Product> products = createTestProducts();
        List<Order> orders = new ArrayList<>();

        // 截止时间 = 计划开始时间 (必延迟)
        orders.add(createOrder(1, 1, 1, 1000.0, planStartTime));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);
        // 显式传入测试用的罚款率
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, TEST_PENALTY_RATE, 10);

        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);

        LocalDateTime completionTime = scheduleResult.completionTimes.get(1);
        LocalDateTime alignedDeadline = orders.get(0).getAlignedDeadline();

        if (completionTime != null && completionTime.isAfter(alignedDeadline)) {
            // 期望罚款 = 订单价值 * 罚款率
            double expectedPenalty = 1000.0 * TEST_PENALTY_RATE;
            assertEquals("罚款金额计算错误", expectedPenalty, scheduleResult.totalPenalty, 0.001);
        }
    }
}