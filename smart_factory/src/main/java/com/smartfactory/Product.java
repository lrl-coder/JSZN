package com.smartfactory;

public class Product {
    private int id; // 产品ID (1, 2, 3)
    private double unitProcessingTime; // 单位加工时间 (这里默认为 4 小时)

    public Product(int id, double unitProcessingTime) {
        this.id = id;
        this.unitProcessingTime = unitProcessingTime;
    }

    // Getters
    public int getId() { return id; }
    public double getUnitProcessingTime() { return unitProcessingTime; }
}