package com.smartfactory;

import java.time.LocalDateTime;

public class Order {
    private int id;
    private int productId;      // 所需产品ID
    private int quantity;       // 数量
    private double totalValue;  // 总价值
    private LocalDateTime deadline;    // 截止时间（原始值）
    private LocalDateTime arrivalTime; // 到达时间

    public Order(int id, int productId, int quantity, double totalValue, LocalDateTime deadline, LocalDateTime arrivalTime) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.totalValue = totalValue;
        this.deadline = deadline;
        this.arrivalTime = arrivalTime;
    }

    // Getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getTotalValue() { return totalValue; }
    
    /**
     * 获取原始截止时间
     */
    public LocalDateTime getDeadline() { return deadline; }
    
    /**
     * 获取对齐后的截止时间（截止日期的当天早上8点）
     * 根据作业条件：合同的截止时间均是指截止日期的当天早上8点
     */
    public LocalDateTime getAlignedDeadline() {
        return deadline.toLocalDate().atTime(8, 0);
    }
    
    public LocalDateTime getArrivalTime() { return arrivalTime; }
}