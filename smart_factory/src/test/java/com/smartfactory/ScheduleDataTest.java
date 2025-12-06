package com.smartfactory;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ScheduleData类的单元测试
 * 优化：使用相对时间，封装数据构建过程
 */
public class ScheduleDataTest {

    // 统一的时间基准
    private final LocalDateTime planStartTime = TestTool.getPlanStartTime();

    /**
     * 辅助方法：快速创建产品
     */
    private Product createProduct(int id, double processTime) {
        return new Product(id, processTime);
    }

    /**
     * 辅助方法：快速创建订单
     */
    private Order createOrder(int id, int productId, LocalDateTime deadline) {
        // 默认到达时间为计划开始时间
        return new Order(id, productId, 10, 1000.0, deadline, planStartTime);
    }

    @Test
    public void testConstants() {
        // 测试常量值，确保核心配置未被意外修改
        assertEquals("生产线数量应为3", 3, ScheduleData.NUM_LINES);
        assertEquals("时间块应为4小时", 4.0, ScheduleData.TIME_BLOCK_HOURS, 0.001);
        assertEquals("罚款率应为0.1", 0.1, ScheduleData.PENALTY_RATE, 0.001);
    }

    @Test
    public void testConstructor() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 4.0));

        List<Order> orders = new ArrayList<>();
        // 使用相对时间：截止时间设为明天
        orders.add(createOrder(1, 1, planStartTime.plusDays(1)));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);

        assertNotNull("产品列表不应为空", data.getProducts());
        assertNotNull("订单列表不应为空", data.getOrders());
        assertEquals(1, data.getProducts().size());
        assertEquals(1, data.getOrders().size());

        // 验证计划开始时间是否正确传入
        assertEquals(planStartTime, data.getPlanStartTime());
    }

    @Test
    public void testGetters() {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1, 4.0));
        products.add(createProduct(2, 3.0));

        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = planStartTime.plusDays(1);

        orders.add(createOrder(1, 1, deadline));
        orders.add(createOrder(2, 2, deadline));

        ScheduleData data = new ScheduleData(products, orders, planStartTime);

        // 验证列表大小
        assertEquals(2, data.getProducts().size());
        assertEquals(2, data.getOrders().size());

        // 验证具体元素内容
        assertEquals("获取的产品ID不匹配", 1, data.getProducts().get(0).getId());
        assertEquals("获取的产品ID不匹配", 2, data.getProducts().get(1).getId());

        assertEquals("获取的订单ID不匹配", 1, data.getOrders().get(0).getId());
        assertEquals("获取的订单ID不匹配", 2, data.getOrders().get(1).getId());
    }

    @Test
    public void testEmptyLists() {
        // 边界测试：空列表初始化
        ScheduleData data = new ScheduleData(new ArrayList<>(), new ArrayList<>(), planStartTime);

        assertNotNull(data.getProducts());
        assertTrue(data.getProducts().isEmpty());

        assertNotNull(data.getOrders());
        assertTrue(data.getOrders().isEmpty());
    }
}