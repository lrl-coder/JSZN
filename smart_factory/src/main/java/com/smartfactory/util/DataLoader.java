package com.smartfactory.util;

import com.smartfactory.Order;
import com.smartfactory.Product;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    // 定义时间格式：例如 "2025-11-26 08:00"
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 加载产品数据
     * CSV格式: id,unitProcessingTime
     */
    public static List<Product> loadProducts(String filePath) {
        List<Product> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // 跳过表头（如果有）
            // br.readLine(); 
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#") || line.startsWith("id")) continue; // 跳过注释或表头
                
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0].trim());
                double time = Double.parseDouble(parts[1].trim());
                
                list.add(new Product(id, time));
            }
        } catch (IOException e) {
            System.err.println("Error reading products file: " + e.getMessage());
        }
        return list;
    }

    /**
     * 加载订单数据
     * CSV格式: id,productId,quantity,totalValue,deadline,arrivalTime
     */
    public static List<Order> loadOrders(String filePath) {
        List<Order> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#") || line.startsWith("id")) continue;

                String[] parts = line.split(",");
                // 简单的防错处理
                if (parts.length < 6) continue;

                int id = Integer.parseInt(parts[0].trim());
                int pId = Integer.parseInt(parts[1].trim());
                int qty = Integer.parseInt(parts[2].trim());
                double val = Double.parseDouble(parts[3].trim());
                LocalDateTime deadline = LocalDateTime.parse(parts[4].trim(), DATE_FMT);
                deadline = deadline.toLocalDate().atTime(8, 0);
                LocalDateTime arrival = LocalDateTime.parse(parts[5].trim(), DATE_FMT);

                list.add(new Order(id, pId, qty, val, deadline, arrival));
            }
        } catch (IOException e) {
            System.err.println("Error reading orders file: " + e.getMessage());
        }
        return list;
    }
}