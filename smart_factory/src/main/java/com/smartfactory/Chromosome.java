package com.smartfactory;

import java.util.List;

/**
 * 染色体结构：包含操作序列和机器分配。
 */
public class Chromosome {
    // 1. 操作序列：存储订单ID和工件序号 (例如: [O1_1, O2_1, O1_2, ...])
    private List<String> operationSequence;

    // 2. 机器分配：与操作序列一一对应 (例如: [Line2, Line3, Line1, ...])
    private List<Integer> machineAssignment;

    // 适应度值：目标函数 (总成本 + 罚款)
    private double fitness = Double.MAX_VALUE;

    public Chromosome(List<String> operationSequence, List<Integer> machineAssignment) {
        this.operationSequence = operationSequence;
        this.machineAssignment = machineAssignment;
    }

    // 调度解码后，记录的调度信息 (方便后续分析和输出)
    // Map<LineId, List<JobCompletionInfo>>

    // --- Getters and Setters for fitness, sequences, and assignments ---
    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }
    public List<String> getOperationSequence() { return operationSequence; }
    public List<Integer> getMachineAssignment() { return machineAssignment; }

    // 注意：需要重写 equals() 和 hashCode() 以便在集合中使用
}