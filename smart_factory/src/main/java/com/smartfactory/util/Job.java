package com.smartfactory.util;

import java.time.LocalDateTime;

public class Job {
    public String operationId; // 例如: "O1_1" (订单1的第一个工件)
    public int productId;
    public int machineLineId;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public double costCoefficient; // 本任务的工资成本系数
    public double baseCost; // 任务的基础工资成本

    public Job(String operationId, int productId, int machineLineId, LocalDateTime startTime,
               LocalDateTime endTime, double costCoefficient, double baseCost) {
        this.operationId = operationId;
        this.productId = productId;
        this.machineLineId = machineLineId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.costCoefficient = costCoefficient;
        this.baseCost = baseCost;
    }
}
