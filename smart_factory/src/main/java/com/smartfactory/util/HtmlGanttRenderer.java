package com.smartfactory.util;

import com.smartfactory.GAScheduler;
import com.smartfactory.Order;
import com.smartfactory.Product;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 负责生成 HTML 格式的甘特图 (修复拼单显示重叠问题版)
 */
public class HtmlGanttRenderer {

    /**
     * 生成 HTML 报告
     * @param result 调度结果
     * @param orders 原始订单列表
     * @param products 产品列表 (新增：用于获取产品实际工时，计算视觉偏移)
     * @param filePath 输出路径
     */
    public static void generate(GAScheduler.ScheduleResult result,
                                List<Order> orders,
                                List<Product> products,
                                String filePath) {
        StringBuilder sb = new StringBuilder();
        List<Job> jobs = new ArrayList<>(result.scheduledJobs);

        // 1. 构建快速查找Map
        Map<Integer, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        // 构建产品工时Map: ProductId -> UnitProcessingTime
        Map<Integer, Double> productTimeMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getUnitProcessingTime));

        // 2. 对任务进行排序：先按生产线，再按开始时间，最后按ID (保证拼单任务挨在一起)
        jobs.sort(Comparator.comparingInt((Job j) -> j.machineLineId)
                .thenComparing(j -> j.startTime)
                .thenComparing(j -> j.operationId));

        // --- HTML 头部 ---
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append("<meta charset='UTF-8'>\n");
        sb.append("<title>智能工厂生产调度甘特图</title>\n");
        sb.append("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background-color: #f9f9f9; }\n");
        sb.append("h1 { color: #333; }\n");
        // 样式定义
        sb.append(".summary-box { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 25px; border-left: 5px solid #2196F3; box-shadow: 0 2px 5px rgba(0,0,0,0.1); display: flex; gap: 40px; }\n");
        sb.append(".stat-item { display: flex; flex-direction: column; }\n");
        sb.append(".stat-label { font-size: 0.9em; color: #666; margin-bottom: 5px; }\n");
        sb.append(".stat-value { font-size: 1.4em; font-weight: bold; color: #333; }\n");
        sb.append(".stat-value.penalty { color: #e53935; }\n");
        sb.append("#timeline { height: 600px; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n");
        sb.append(".footer-note { margin-top: 10px; font-size: 0.85em; color: #888; text-align: right; }\n");
        sb.append("</style>\n");
        sb.append("</head>\n<body>\n");

        // --- 摘要信息 ---
        sb.append("<h1>🏭 智能工厂生产调度结果</h1>\n");
        sb.append("<div class='summary-box'>\n");
        sb.append("<div class=\"stat-item\"><span class=\"stat-label\">总利润</span><span class=\"stat-value\">").append(String.format("%.2f", -result.totalCost)).append(" 元</span></div>\n");
        String penaltyClass = result.totalPenalty > 0 ? "stat-value penalty" : "stat-value";
        String warningIcon = result.totalPenalty > 0 ? " ⚠️" : "";
        sb.append("<div class=\"stat-item\"><span class=\"stat-label\">总罚款</span><span class=\"").append(penaltyClass).append("\">").append(String.format("%.2f", result.totalPenalty)).append(" 元").append(warningIcon).append("</span></div>\n");
        sb.append("<div class=\"stat-item\"><span class=\"stat-label\">总任务数</span><span class=\"stat-value\">").append(jobs.size()).append(" 个</span></div>\n");
        sb.append("</div>\n");

        // --- 图表容器 ---
        sb.append("<div id=\"timeline\"></div>\n");
        sb.append("<div class=\"footer-note\">提示：拼单任务已按实际工时展开显示。带 ★ 为免费拼单。</div>\n");

        // --- JS 脚本 ---
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("google.charts.load('current', {'packages':['timeline']});\n");
        sb.append("google.charts.setOnLoadCallback(drawChart);\n");
        sb.append("function drawChart() {\n");
        sb.append("  var container = document.getElementById('timeline');\n");
        sb.append("  var chart = new google.visualization.Timeline(container);\n");
        sb.append("  var dataTable = new google.visualization.DataTable();\n");

        sb.append("  dataTable.addColumn({ type: 'string', id: 'Line' });\n");
        sb.append("  dataTable.addColumn({ type: 'string', id: 'Order' });\n");
        sb.append("  dataTable.addColumn({ type: 'string', role: 'tooltip' });\n");
        sb.append("  dataTable.addColumn({ type: 'date', id: 'Start' });\n");
        sb.append("  dataTable.addColumn({ type: 'date', id: 'End' });\n");
        sb.append("  dataTable.addRows([\n");

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        // --- 核心：视觉偏移计算逻辑 ---
        // 记录每条生产线当前绘制到了什么时间点
        Map<Integer, LocalDateTime> lineVisualOffset = new HashMap<>();

        for (Job job : jobs) {
            int lineId = job.machineLineId;
            // 获取该Job在逻辑上的块开始时间（例如 08:00）
            LocalDateTime blockStart = job.startTime;

            // 获取该生产线当前的视觉游标
            LocalDateTime currentCursor = lineVisualOffset.getOrDefault(lineId, blockStart);

            // 计算视觉上的开始时间：
            // 如果游标比块开始时间大（说明这是拼单的后续任务），就从游标开始画
            // 如果游标比块开始时间小（说明这是新的一块，或者有空闲），就从块开始时间画
            LocalDateTime visualStart;
            if (currentCursor.isAfter(blockStart)) {
                visualStart = currentCursor;
            } else {
                visualStart = blockStart;
            }

            // 获取该产品的实际加工时长 (例如 2.0小时 或 1.0小时)
            Double durationHours = productTimeMap.getOrDefault(job.productId, 4.0);
            long durationSeconds = (long)(durationHours * 3600);

            // 计算视觉上的结束时间
            LocalDateTime visualEnd = visualStart.plusSeconds(durationSeconds);

            // 更新游标，供下一个拼单任务使用
            lineVisualOffset.put(lineId, visualEnd);

            // --- 下面是生成HTML代码 (使用 visualStart 和 visualEnd) ---
            String jsStartDate = toJsDate(visualStart);
            String jsEndDate = toJsDate(visualEnd);

            int orderId = Integer.parseInt(job.operationId.split("_")[0].substring(1));
            Order order = orderMap.get(orderId);

            boolean isLate = false;
            LocalDateTime finishTime = result.completionTimes.get(orderId);
            if (finishTime != null && order != null && finishTime.isAfter(order.getAlignedDeadline())) {
                isLate = true;
            }

            String label = job.operationId + " (P" + job.productId + ")";
            if (job.baseCost == 0.0) label = "★ " + label;
            if (isLate) label += " ⚠️";

            StringBuilder tooltip = new StringBuilder();
            tooltip.append("任务: ").append(job.operationId).append("\\n");
            tooltip.append("产品: P").append(job.productId).append("\\n");
            // Tooltip 显示真实的视觉时间段
            tooltip.append("实际排程: ").append(visualStart.format(fullFmt))
                    .append(" ~ ").append(visualEnd.format(timeFmt)).append("\\n");

            // 同时显示它所属的计费块信息，方便理解
            if (visualStart.isAfter(blockStart)) {
                tooltip.append("(所属计费块起始: ").append(blockStart.format(timeFmt)).append(")\\n");
            }

            if (job.baseCost == 0.0) {
                tooltip.append("成本: 0 (拼单免费)");
            } else {
                tooltip.append("成本: ").append((int)job.baseCost).append(" (系数 ").append(job.costCoefficient).append(")");
            }
            if (isLate) tooltip.append("\\n[⚠️ 延误]");

            sb.append(String.format("    ['生产线 %d', '%s', '%s', %s, %s],\n",
                    lineId, label, tooltip.toString(), jsStartDate, jsEndDate));
        }

        sb.append("  ]);\n");
        sb.append("  var options = { timeline: { showRowLabels: true, groupByRowLabel: true, rowLabelStyle: { fontName: 'Segoe UI', fontSize: 14, color: '#333' }, barLabelStyle: { fontName: 'Segoe UI', fontSize: 12 } }, backgroundColor: '#fff' };\n");
        sb.append("  chart.draw(dataTable, options);\n");
        sb.append("}\n");
        sb.append("</script>\n");
        sb.append("</body>\n</html>");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
            System.out.println("✅ 可视化甘特图已生成: " + filePath);
        } catch (IOException e) {
            System.err.println("❌ 生成甘特图失败: " + e.getMessage());
        }
    }

    private static String toJsDate(LocalDateTime dt) {
        return String.format("new Date(%d, %d, %d, %d, %d, %d)",
                dt.getYear(), dt.getMonthValue() - 1, dt.getDayOfMonth(), dt.getHour(), dt.getMinute(), dt.getSecond());
    }
}