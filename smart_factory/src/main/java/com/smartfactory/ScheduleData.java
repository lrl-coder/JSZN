package com.smartfactory;

import java.util.List;

public class ScheduleData {
    public static final int NUM_LINES = 3; // 生产线数量
    public static final double TIME_BLOCK_HOURS = 4.0; // 换产品时间间隔
    public static final double PENALTY_RATE = 0.1; // 订单罚款比例

    private List<Product> products;
    private List<Order> orders;

    public ScheduleData(List<Product> products, List<Order> orders) {
        this.products = products;
        this.orders = orders;
    }

    // Getters
    public List<Product> getProducts() { return products; }
    public List<Order> getOrders() { return orders; }
}
