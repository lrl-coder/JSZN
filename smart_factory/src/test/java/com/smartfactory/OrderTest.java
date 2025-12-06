package com.smartfactory;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;

/**
 * Order类的单元测试
 * 重点测试截止时间对齐功能
 */
public class OrderTest {

    @Test
    public void testGetAlignedDeadline_Morning8AM() {
        // 测试：截止时间正好是早上8点
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 8, 0);
        Order order = new Order(1, 1, 10, 1000.0, deadline, 
            LocalDateTime.of(2025, 11, 26, 8, 0));
        
        LocalDateTime aligned = order.getAlignedDeadline();
        assertEquals(LocalDateTime.of(2025, 11, 27, 8, 0), aligned);
    }

    @Test
    public void testGetAlignedDeadline_Afternoon() {
        // 测试：截止时间是下午，应该对齐到当天早上8点
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 14, 30);
        Order order = new Order(1, 1, 10, 1000.0, deadline, 
            LocalDateTime.of(2025, 11, 26, 8, 0));
        
        LocalDateTime aligned = order.getAlignedDeadline();
        assertEquals(LocalDateTime.of(2025, 11, 27, 8, 0), aligned);
    }

    @Test
    public void testGetAlignedDeadline_Night() {
        // 测试：截止时间是晚上，应该对齐到当天早上8点
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 23, 59);
        Order order = new Order(1, 1, 10, 1000.0, deadline, 
            LocalDateTime.of(2025, 11, 26, 8, 0));
        
        LocalDateTime aligned = order.getAlignedDeadline();
        assertEquals(LocalDateTime.of(2025, 11, 27, 8, 0), aligned);
    }

    @Test
    public void testGetAlignedDeadline_EarlyMorning() {
        // 测试：截止时间是凌晨，应该对齐到当天早上8点
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 3, 0);
        Order order = new Order(1, 1, 10, 1000.0, deadline, 
            LocalDateTime.of(2025, 11, 26, 8, 0));
        
        LocalDateTime aligned = order.getAlignedDeadline();
        assertEquals(LocalDateTime.of(2025, 11, 27, 8, 0), aligned);
    }

    @Test
    public void testGetAlignedDeadline_Exactly8AM() {
        // 测试：截止时间正好是早上8点整
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 8, 0, 0);
        Order order = new Order(1, 1, 10, 1000.0, deadline, 
            LocalDateTime.of(2025, 11, 26, 8, 0));
        
        LocalDateTime aligned = order.getAlignedDeadline();
        assertEquals(LocalDateTime.of(2025, 11, 27, 8, 0, 0), aligned);
    }

    @Test
    public void testGetAlignedDeadline_Before8AM() {
        // 测试：截止时间是早上8点之前，应该对齐到当天早上8点
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 7, 59);
        Order order = new Order(1, 1, 10, 1000.0, deadline, 
            LocalDateTime.of(2025, 11, 26, 8, 0));
        
        LocalDateTime aligned = order.getAlignedDeadline();
        assertEquals(LocalDateTime.of(2025, 11, 27, 8, 0), aligned);
    }

    @Test
    public void testGetters() {
        // 测试：所有getter方法
        LocalDateTime deadline = LocalDateTime.of(2025, 11, 27, 14, 0);
        LocalDateTime arrival = LocalDateTime.of(2025, 11, 26, 8, 0);
        Order order = new Order(1, 2, 5, 500.0, deadline, arrival);
        
        assertEquals(1, order.getId());
        assertEquals(2, order.getProductId());
        assertEquals(5, order.getQuantity());
        assertEquals(500.0, order.getTotalValue(), 0.001);
        assertEquals(deadline, order.getDeadline());
        assertEquals(arrival, order.getArrivalTime());
    }
}

