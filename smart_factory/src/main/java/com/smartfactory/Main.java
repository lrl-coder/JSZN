package com.smartfactory;

import com.smartfactory.util.DataLoader;
import com.smartfactory.util.HtmlGanttRenderer;
import com.smartfactory.util.Job;
import com.smartfactory.util.TimeCostUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- 智能制造生产调度系统 (文件输入版) ---");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today8AM = now.toLocalDate().atTime(8, 0);
        LocalDateTime planStartTime = today8AM;  // 使用今天的8点作为排程基准;

        System.out.println("当前排程基准时间: " + planStartTime);
        System.out.println("说明：只处理在 " + planStartTime + " 之前(或等于)到达的订单");

        // 2. 从文件加载数据
        String productFile = "input/run/products.csv";
        String orderFile = "input/run/orders.csv";

        List<Product> products = DataLoader.loadProducts(productFile);
        List<Order> allOrders = DataLoader.loadOrders(orderFile);

        if (products.isEmpty() || allOrders.isEmpty()) {
            System.err.println("数据加载失败，请检查 csv 文件路径和内容格式！");
            return;
        }

        System.out.println("已加载产品类型数: " + products.size());
        System.out.println("已加载总订单数: " + allOrders.size());

        // 3. 过滤订单：
        // 规则：只处理在 planStartTime 之前(或等于)到达的订单
        // 8点之后的订单留在明天处理 (即不进入本次 ScheduleData)
        List<Order> activeOrders = allOrders.stream()
                .filter(o -> !o.getArrivalTime().isAfter(planStartTime))
                .collect(Collectors.toList());

        System.out.println("符合本次排程条件的订单数: " + activeOrders.size());

        if (activeOrders.isEmpty()) {
            System.out.println("当前没有需要处理的订单。");
            return;
        }

        // 4. 构建调度数据上下文
        ScheduleData data = new ScheduleData(products, activeOrders, planStartTime);

        // 5. 运行遗传算法
        // 参数：种群50，交叉0.8，变异0.2，迭代100代
        GAScheduler scheduler = new GAScheduler(data, 200, 0.8, 0.2, 300);
        Chromosome bestSolution = scheduler.run();

        // 6. 输出结果
        System.out.println("\n--- 优化完成 ---");
        if (bestSolution != null) {
            GAScheduler.ScheduleResult result = scheduler.getDetailedSchedule(bestSolution);
            System.out.println("最佳方案总利润 (含罚款): " + -result.totalCost);

            System.out.println("其中包含罚款总额: " + result.totalPenalty); // 显示总罚款

            // 打印订单详情表
            printOrderReport(activeOrders, result.completionTimes);

            // 简单的文本可视化
            printSchedule(result.scheduledJobs);

            // 【新增】生成 HTML 甘特图
            HtmlGanttRenderer.generate(result, activeOrders, products, "schedule_report.html");

            System.out.println("请使用浏览器打开 schedule_report.html 查看可视化结果。");
        }
    }

    private static void printSchedule(List<Job> jobs) {
        // 按机器分组
        Map<Integer, List<Job>> jobsByLine = jobs.stream()
                .collect(Collectors.groupingBy(j -> j.machineLineId));

        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm");

        System.out.println("\n=== 详细调度甘特表 ===");

        // 遍历所有生产线 (1 到 3)
        for (int line = 1; line <= ScheduleData.NUM_LINES; line++) {
            System.out.println("\n[生产线 " + line + "]");
            List<Job> lineJobs = jobsByLine.getOrDefault(line, Collections.emptyList());

            // 按时间排序
            lineJobs.sort(Comparator.comparing(j -> j.startTime));

            if (lineJobs.isEmpty()) {
                System.out.println("  (无任务)");
                continue;
            }

            // 表头
            System.out.printf("  %-10s | %-15s | %-15s | %-6s | %-8s | %s%n",
                    "TaskID", "Start", "End", "Prod", "BaseCost", "Note");
            System.out.println("  --------------------------------------------------------------------------------------");

            for (Job job : lineJobs) {
                String note = "";
                if (job.baseCost == 0.0) {
                    // 重点：高亮显示拼单任务
                    note = "★ 拼单成功 (Free)";
                } else {
                    String period = getPeriodName(job.startTime.getHour());
                    note = String.format("新块 (x%.1f %s)", job.costCoefficient, period);
                }

                System.out.printf("  %-10s | %-15s | %-15s | P%-5d | %-8.0f | %s%n",
                        job.operationId,
                        job.startTime.format(timeFmt),
                        job.endTime.format(timeFmt),
                        job.productId,
                        job.baseCost,
                        note);
            }
        }
        System.out.println("========================\n");
    }

    // 辅助显示时段名称
    private static String getPeriodName(int hour) {
        if (hour >= 8 && hour < 16) return "白班";
        if (hour >= 16 && hour < 20) return "晚班";
        if (hour >= 20 || hour == 0) return "夜班";
        return "深夜";
    }

    private static void printOrderReport(List<Order> orders, Map<Integer, LocalDateTime> completionTimes) {
        System.out.println("\n=== 订单完成情况统计表 ===");
        // 打印表头
        System.out.printf("%-5s | %-10s | %-16s | %-16s | %-6s | %s%n",
                "ID", "Value", "Deadline", "FinishTime", "Late?", "Penalty");
        System.out.println("----------------------------------------------------------------------------");

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (Order o : orders) {
            LocalDateTime finish = completionTimes.get(o.getId());

            String finishStr = (finish != null) ? finish.format(fmt) : "UNFINISHED";
            // 显示对齐后的截止时间（当天早上8点）
            LocalDateTime alignedDeadline = o.getAlignedDeadline();
            String deadlineStr = alignedDeadline.format(fmt);

            String isLate = "No";
            double penalty = 0.0;

            // 判断是否延误（使用对齐后的截止时间）
            if (finish != null && finish.isAfter(alignedDeadline)) {
                isLate = "YES"; // 延误标记
                penalty = o.getTotalValue() * ScheduleData.PENALTY_RATE;
            }

            System.out.printf("%-5d | %-10.0f | %-16s | %-16s | %-6s | %-8.0f%n",
                    o.getId(), o.getTotalValue(), deadlineStr, finishStr, isLate, penalty);
        }
        System.out.println("==============================\n");
    }
}