package com.smartfactory;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ScheduleData类的单元测试
 */
public class ScheduleDataTest {

    @Test
    public void testConstants() {
        // 测试常量值
        assertEquals(3, ScheduleData.NUM_LINES);
        assertEquals(4.0, ScheduleData.TIME_BLOCK_HOURS, 0.001);
        assertEquals(0.1, ScheduleData.PENALTY_RATE, 0.001);
    }

    @Test
    public void testConstructor() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 8, 0);
        LocalDateTime arrival = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 10, 1000.0, deadline, arrival));
        
        ScheduleData data = new ScheduleData(products, orders);
        
        assertNotNull(data.getProducts());
        assertNotNull(data.getOrders());
        assertEquals(1, data.getProducts().size());
        assertEquals(1, data.getOrders().size());
    }

    @Test
    public void testGetters() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, 4.0));
        products.add(new Product(2, 3.0));
        
        List<Order> orders = new ArrayList<>();
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 8, 0);
        LocalDateTime arrival = LocalDateTime.of(2025, 11, 26, 8, 0);
        orders.add(new Order(1, 1, 10, 1000.0, deadline, arrival));
        orders.add(new Order(2, 2, 5, 500.0, deadline, arrival));
        
        ScheduleData data = new ScheduleData(products, orders);
        
        assertEquals(2, data.getProducts().size());
        assertEquals(2, data.getOrders().size());
        assertEquals(1, data.getProducts().get(0).getId());
        assertEquals(1, data.getOrders().get(0).getId());
    }
}

