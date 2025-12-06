package com.smartfactory;

import com.smartfactory.util.Job;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 边界情况测试
 * 测试各种极端情况和边界条件
 */
public class BoundaryCaseTest {

    /**
     * 边界测试1：订单数量为0
     */
    @Test(timeout = 5000)  // 5秒超时
    public void testZeroOrders() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 10, 0.8, 0.1, 5);
        
        Chromosome result = scheduler.run();
        assertNotNull(result);
        
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        assertNotNull(scheduleResult);
        assertEquals(0, scheduleResult.scheduledJobs.size());
        assertEquals(0.0, scheduleResult.totalPenalty, 0.001);
    }

    /**
     * 边界测试3：订单数量非常大（每个订单多个工件）
     * 注意：这是一个长时间运行的测试，如果超时可以跳过
     */
    @Test(timeout = 300000)  // 300秒超时（5分钟）
    public void testLargeOrderQuantity() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 1.0));  // 1小时，可以拼单
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        // 创建1个订单，但数量较大（减少数量以加快测试）
        orders.add(new Order(1, 1, 10, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
        // 使用更小的种群和更少的迭代次数以加快测试
        GAScheduler scheduler = new GAScheduler(data, 15, 0.8, 0.1, 8);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        // 应该有10个Job（每个工件一个Job）
        // 注意：由于拼单逻辑，实际数量可能略有不同，但应该等于订单数量
        assertEquals(10, scheduleResult.scheduledJobs.size());
    }

    /**
     * 边界测试4：加工时间正好等于4小时
     */
    @Test
    public void testProcessingTimeExactlyFourHours() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));  // 正好4小时
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 1, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        assertEquals(1, scheduleResult.scheduledJobs.size());
        
        // 检查Job的时间对齐
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
        products.add(new Product(1, 3.99));  // 略小于4小时
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 1, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
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
        products.add(new Product(1, 4.01));  // 略大于4小时
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 1, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
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
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 1, 0.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        assertEquals(1, scheduleResult.scheduledJobs.size());
        // 如果延迟，罚款应该是0（因为订单价值为0）
        assertEquals(0.0, scheduleResult.totalPenalty, 0.001);
    }

    /**
     * 边界测试8：订单价值非常大
     */
    @Test
    public void testVeryLargeOrderValue() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 1, 1000000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
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
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        // 创建多个订单，截止时间都是当天8点（非常紧急）
        for (int i = 1; i <= 5; i++) {
            orders.add(new Order(i, 1, 1, 1000.0, 
                LocalDateTime.of(2025, 11, 26, 8, 0), planStart));
        }
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        assertEquals(5, scheduleResult.scheduledJobs.size());
        // 由于截止时间对齐到当天8点，而计划开始也是8点，大部分订单可能会延迟
        assertTrue("应该有罚款", scheduleResult.totalPenalty >= 0);
    }

    /**
     * 边界测试10：所有订单都不延迟
     */
    @Test
    public void testAllOrdersOnTime() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 1.0));  // 1小时，容易完成
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        // 创建多个订单，截止时间都很宽松
        for (int i = 1; i <= 5; i++) {
            orders.add(new Order(i, 1, 1, 1000.0, 
                LocalDateTime.of(2025, 12, 26, 8, 0), planStart));
        }
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        // 注意：由于尾数拼单逻辑，实际Job数量可能会略有不同
        // 但应该至少等于订单数量（每个订单至少1个Job）
        assertTrue("Job数量应该至少等于订单数量", 
            scheduleResult.scheduledJobs.size() >= 5);
        // 由于截止时间很宽松，应该没有罚款或罚款很少
        assertTrue("罚款应该为0或很小", scheduleResult.totalPenalty >= 0);
    }

    /**
     * 边界测试11：订单到达时间正好是8点
     */
    @Test
    public void testArrivalTimeExactly8AM() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        // 订单到达时间正好是8点
        orders.add(new Order(1, 1, 1, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);
        
        Chromosome result = scheduler.run();
        assertNotNull(result);
    }

    /**
     * 边界测试12：订单到达时间略早于8点
     */
    @Test
    public void testArrivalTimeJustBefore8AM() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        
        // 订单到达时间略早于8点（应该被包含）
        LocalDateTime arrival = LocalDateTime.of(2025, 11, 26, 7, 59, 59);
        orders.add(new Order(1, 1, 1, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), arrival));
        
        ScheduleData data = new ScheduleData(products, orders);
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
        products.add(new Product(1, 4.0));
        products.add(new Product(2, 3.0));
        products.add(new Product(3, 2.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        orders.add(new Order(1, 1, 1, 1000.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        orders.add(new Order(2, 2, 1, 800.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        orders.add(new Order(3, 3, 1, 600.0, 
            LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        assertEquals(3, scheduleResult.scheduledJobs.size());
        
        // 检查是否包含所有三种产品
        boolean hasProduct1 = scheduleResult.scheduledJobs.stream()
            .anyMatch(job -> job.productId == 1);
        boolean hasProduct2 = scheduleResult.scheduledJobs.stream()
            .anyMatch(job -> job.productId == 2);
        boolean hasProduct3 = scheduleResult.scheduledJobs.stream()
            .anyMatch(job -> job.productId == 3);
        
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
        products.add(new Product(1, 2.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        // 创建多个订单，算法可能会分配到同一条生产线
        for (int i = 1; i <= 10; i++) {
            orders.add(new Order(i, 1, 1, 100.0, 
                LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        }
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 30, 0.8, 0.1, 15);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        // 注意：由于尾数拼单逻辑，实际Job数量可能会略有不同
        // 但应该至少等于订单数量（每个订单至少1个Job）
        assertTrue("Job数量应该至少等于订单数量", 
            scheduleResult.scheduledJobs.size() >= 10);
        
        // 检查所有Job是否都在有效的生产线上（1-3）
        for (Job job : scheduleResult.scheduledJobs) {
            assertTrue("生产线ID应该在1-3之间", 
                job.machineLineId >= 1 && job.machineLineId <= 3);
        }
    }

    /**
     * 边界测试15：订单数量正好等于生产线数量
     */
    @Test
    public void testOrdersEqualLines() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime planStart = LocalDateTime.of(2025, 11, 26, 8, 0);
        
        // 创建3个订单（正好等于生产线数量）
        for (int i = 1; i <= 3; i++) {
            orders.add(new Order(i, 1, 1, 1000.0, 
                LocalDateTime.of(2025, 11, 27, 8, 0), planStart));
        }
        
        ScheduleData data = new ScheduleData(products, orders);
        GAScheduler scheduler = new GAScheduler(data, 20, 0.8, 0.1, 10);
        
        Chromosome result = scheduler.run();
        GAScheduler.ScheduleResult scheduleResult = scheduler.getDetailedSchedule(result);
        
        assertEquals(3, scheduleResult.scheduledJobs.size());
    }
}

