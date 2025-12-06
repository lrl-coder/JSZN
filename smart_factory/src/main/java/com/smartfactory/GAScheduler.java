package com.smartfactory;

import com.smartfactory.util.Job;
import com.smartfactory.util.TimeCostUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GAScheduler {
    private ScheduleData data;
    private int populationSize;
    private double crossoverRate;
    private double mutationRate;
    private int maxGenerations;
    private Random random = new Random();
    // 记录连续未进化代数，用于自适应调整
    private int stagnationCount = 0;

    public GAScheduler(ScheduleData data, int popSize, double crossRate, double mutRate, int maxGen) {
        this.data = data;
        this.populationSize = popSize;
        this.crossoverRate = crossRate;
        this.mutationRate = mutRate;
        this.maxGenerations = maxGen;
    }

    /**
     * 遗传算法主入口
     */
    public Chromosome run() {
        // 检查是否有订单需要处理
        if (data.getOrders().isEmpty()) {
            // 返回一个空的染色体
            return new Chromosome(new ArrayList<>(), new ArrayList<>());
        }
        
        // 1. 初始化种群
        List<Chromosome> population = initializePopulation();

        // 初始评估
        evaluatePopulationFitness(population);

        Chromosome bestSolution = getBest(population);
        if (bestSolution == null) {
            // 如果种群为空，返回一个空的染色体
            return new Chromosome(new ArrayList<>(), new ArrayList<>());
        }
        double bestFitness = bestSolution.getFitness();

        for (int gen = 0; gen < maxGenerations; gen++) {

            // --- [改进点 1: 自适应参数控制] ---
            adjustMutationRate();

            // 2. 选择
            List<Chromosome> parents = selection(population);

            // 3. 交叉变异
            List<Chromosome> newPopulation = newGeneration(parents);

            // 4. 评估新种群
            evaluatePopulationFitness(newPopulation);

            // --- [改进点 2: 混合算法 - 引入局部搜索] ---
            // 仅对新种群中最好的前 5% 个体进行微调（避免计算量过大）
            // 这就是所谓的 Memetic Algorithm 或 GA-LocalSearch 混合
            performLocalSearchOnElites(newPopulation);

            // 更新全局最优解
            Chromosome currentBest = getBest(newPopulation);
            if (currentBest.getFitness() < bestFitness) {
                bestSolution = currentBest; // 这里实际上需要深拷贝，简化起见直接引用
                bestFitness = currentBest.getFitness();
                stagnationCount = 0; // 进化了，重置停滞计数
                System.out.printf("Generation %d: New Best Cost Found -> %.2f%n", gen, -bestFitness);
            } else {
                stagnationCount++; // 没进化，计数+1
            }

            // 简单的日志，每10代或者发现新解时打印
            if (gen % 10 == 0 || stagnationCount == 0) {
                System.out.println("Generation " + gen + " Best: " + -bestFitness + " (Mutation Rate: " + String.format("%.2f", mutationRate) + ")");
            }

            population = newPopulation;
        }

        return bestSolution;
    }

    // 获取当前种群最优个体的辅助方法
    private Chromosome getBest(List<Chromosome> pop) {
        return pop.stream().min(Comparator.comparingDouble(Chromosome::getFitness)).orElse(null);
    }

    /**
     * [新增] 自适应调整变异率
     * 如果连续 10 代没有进化，就大幅增加变异率，试图跳出局部最优。
     * 一旦进化了，就恢复默认值。
     */
    private void adjustMutationRate() {
        double baseRate = 0.1; // 基础变异率
        if (stagnationCount > 10) {
            this.mutationRate = Math.min(0.5, this.mutationRate + 0.05); // 逐步提升，最高 0.5
        } else {
            this.mutationRate = baseRate; // 恢复正常
        }
    }

    /**
     * [新增] 局部搜索 (Local Search) - 核心改进
     * 对优秀的个体尝试“微调机器分配”。
     * 策略：随机选一个任务，尝试把它换到另一条生产线上。如果成本降低，就保留这个改动。
     */
    private void performLocalSearchOnElites(List<Chromosome> population) {
        // 先按适应度排序
        population.sort(Comparator.comparingDouble(Chromosome::getFitness));

        // 只取前 5 个最好的个体进行精细打磨
        int eliteCount = Math.min(population.size(), 5);

        for (int i = 0; i < eliteCount; i++) {
            hybridLocalSearch(population.get(i));
        }
    }

    /**
     * [改进版] 初始化种群：混合策略
     * 40% 基于产品分组（利于拼单）
     * 30% 基于截止时间（利于由急单）
     * 20% 完全随机
     */
    private List<Chromosome> initializePopulation() {
        List<Chromosome> population = new ArrayList<>(populationSize);

        // 1. 准备基础数据
        List<String> allOperations = new ArrayList<>();
        // 辅助列表：用于创建启发式解
        List<JobObj> rawJobs = new ArrayList<>();

        for (Order order : data.getOrders()) {
            for (int i = 1; i <= order.getQuantity(); i++) {
                String opId = "O" + order.getId() + "_" + i;
                allOperations.add(opId);
                // 使用对齐后的截止时间进行排序
                rawJobs.add(new JobObj(opId, order.getProductId(), order.getAlignedDeadline()));
            }
        }
        int totalJobs = allOperations.size();

        // 2. 生成个体
        for (int i = 0; i < populationSize; i++) {
            List<String> opSequence;
            List<Integer> machineAssignment = new ArrayList<>(totalJobs);

            // --- 策略注入 ---
            if (i < populationSize * 0.4) {
                // 策略A: 按产品ID排序 (通过聚类促进拼单)
                opSequence = rawJobs.stream()
                        .sorted(Comparator.comparingInt(j -> j.productId))
                        .map(j -> j.opId)
                        .collect(Collectors.toList());
            } else if (i < populationSize * 0.7) {
                // 策略B: 按截止时间排序 (优先处理急单)
                opSequence = rawJobs.stream()
                        .sorted(Comparator.comparing(j -> j.deadline))
                        .map(j -> j.opId)
                        .collect(Collectors.toList());
            } else {
                // 策略C: 完全随机
                opSequence = new ArrayList<>(allOperations);
                Collections.shuffle(opSequence, random);
            }

            // 机器分配初始化 (依然保持随机，或者也可以设计轮询分配)
            for (int j = 0; j < totalJobs; j++) {
                machineAssignment.add(random.nextInt(ScheduleData.NUM_LINES) + 1);
            }

            population.add(new Chromosome(opSequence, machineAssignment));
        }
        return population;
    }

    // 辅助内部类，用于初始化排序
    private static class JobObj {
        String opId;
        int productId;
        LocalDateTime deadline;
        JobObj(String o, int p, LocalDateTime d) { opId=o; productId=p; deadline=d; }
    }

    /**
     * 适应度评估：根据染色体计算调度，并计算总成本 (重点函数)
     */
    private void evaluatePopulationFitness(List<Chromosome> population) {
        for (Chromosome c : population) {
            double cost = decodeAndCalculateCost(c);
            c.setFitness(cost);
        }
    }

    /**
     * [终极混合] VNS + SA + TS 三重混合搜索
     * 策略：
     * 1. VNS 控制邻域结构的切换 (k)。
     * 2. SA 控制劣解的接受概率 (Temperature)。
     * 3. TS 避免近期重复操作 (Tabu List)。
     */
    private void hybridLocalSearch(Chromosome c) {
        // --- 1. 参数初始化 ---
        // SA 参数
        double temperature = 200.0;   // 初始温度
        double coolingRate = 0.95;    // 降温速率
        double minTemperature = 1.0;  // 终止温度

        // TS 参数
        Queue<String> tabuList = new LinkedList<>();
        int tabuTenure = 10;          // 禁忌步长

        // 全局最优记录 (兜底用)
        double globalBestCost = c.getFitness();
        List<Integer> bestMa = new ArrayList<>(c.getMachineAssignment());
        List<String> bestOs = new ArrayList<>(c.getOperationSequence());

        // --- 2. 主循环 (SA 退火过程) ---
        while (temperature > minTemperature) {
            int k = 1;
            int maxK = 3; // 定义3种邻域结构

            // VNS 循环：在当前温度下，尝试不同的邻域
            while (k <= maxK) {
                // 备份当前状态 (用于回滚)
                List<Integer> currentMa = new ArrayList<>(c.getMachineAssignment());
                List<String> currentOs = new ArrayList<>(c.getOperationSequence());
                double currentCost = c.getFitness();

                // --- A. 产生邻域扰动 (Perturbation) ---
                String moveKey = applyPerturbationAndGetKey(c, k);

                // --- B. 计算新适应度 ---
                double newCost = decodeAndCalculateCost(c);
                double delta = newCost - currentCost;

                // --- C. 混合判断逻辑 ---
                boolean isTabu = tabuList.contains(moveKey);
                boolean isAspiration = (newCost < globalBestCost); // 渴望准则：打破历史最优

                boolean accept = false;

                if (isTabu && !isAspiration) {
                    // 1. 命中禁忌且未满足渴望准则 -> 强制拒绝 (尽管 SA 可能会接受，但 TS 说不行)
                    accept = false;
                } else {
                    // 2. 非禁忌，或者满足渴望准则 -> 进入 SA 判断
                    if (delta < 0) {
                        accept = true; // 更好，直接接受
                    } else {
                        // 更差，按 Metropolis 准则概率接受
                        if (random.nextDouble() < Math.exp(-delta / temperature)) {
                            accept = true;
                        }
                    }
                }

                // --- D. 执行决策 ---
                if (accept) {
                    // 确认接受新解
                    c.setFitness(newCost);

                    // 更新全局最优
                    if (newCost < globalBestCost) {
                        globalBestCost = newCost;
                        bestMa = new ArrayList<>(c.getMachineAssignment());
                        bestOs = new ArrayList<>(c.getOperationSequence());
                    }

                    // 加入禁忌表
                    tabuList.add(moveKey);
                    if (tabuList.size() > tabuTenure) {
                        tabuList.poll();
                    }

                    // VNS 策略：如果找到了改进(或被SA接受的移动)，回到第一个邻域继续深挖 (Exploitation)
                    // 也可以选择 k 不变，或者 k=1。这里选择 k=1 以强化局部搜索能力。
                    k = 1;
                } else {
                    // 拒绝新解：回滚
                    Collections.copy(c.getMachineAssignment(), currentMa);
                    Collections.copy(c.getOperationSequence(), currentOs);
                    c.setFitness(currentCost);

                    // VNS 策略：当前邻域没找到路，切换到下一个邻域尝试 (Exploration)
                    k++;
                }
            }

            // 降温
            temperature *= coolingRate;
        }

        // --- 3. 收尾：恢复历史最优 ---
        // 防止 SA/VNS 在最后阶段停留在一个较差的解
        c.setFitness(globalBestCost);
        Collections.copy(c.getMachineAssignment(), bestMa);
        Collections.copy(c.getOperationSequence(), bestOs);
    }

    /**
     * 辅助方法：执行扰动并返回该操作的"禁忌特征码"
     */
    private String applyPerturbationAndGetKey(Chromosome c, int k) {
        int size = c.getOperationSequence().size();
        String key = "";

        switch (k) {
            case 1: // [机器变更]
                int idx = random.nextInt(size);
                int oldM = c.getMachineAssignment().get(idx);
                int newM = random.nextInt(ScheduleData.NUM_LINES) + 1;
                while (newM == oldM) {
                    newM = random.nextInt(ScheduleData.NUM_LINES) + 1;
                }
                c.getMachineAssignment().set(idx, newM);
                // 禁忌特征：禁止将该任务移回原机器 (防止反复横跳)
                key = "MACH_" + idx + "_" + oldM;
                break;

            case 2: // [工序交换]
                int s1 = random.nextInt(size);
                int s2 = random.nextInt(size);
                Collections.swap(c.getOperationSequence(), s1, s2);
                // 禁忌特征：禁止再次交换这两个位置 (防止换回去)
                int min = Math.min(s1, s2);
                int max = Math.max(s1, s2);
                key = "SWAP_" + min + "_" + max;
                break;

            case 3: // [工序插入] (可选)
                int from = random.nextInt(size);
                int to = random.nextInt(size);
                if (from != to) {
                    String op = c.getOperationSequence().remove(from);
                    c.getOperationSequence().add(to, op);
                    Integer mach = c.getMachineAssignment().remove(from);
                    c.getMachineAssignment().add(to, mach);
                }
                // 插入操作较复杂，禁忌特征可以简单定义为禁止操作该工序
                key = "INS_" + from;
                break;
        }
        return key;
    }

    /**
     * 执行特定邻域结构的搜索
     * @return 是否找到了更优解
     */
    private boolean applyNeighborhoodSearch(Chromosome c, int k) {
        double currentCost = c.getFitness();
        boolean improved = false;
        // 备份当前状态（用于回滚）
        List<Integer> startAssignment = new ArrayList<>(c.getMachineAssignment());
        List<String> startSequence = new ArrayList<>(c.getOperationSequence());

        int attempts = 10; // 每个邻域尝试次数
        int size = c.getOperationSequence().size();

        for (int i = 0; i < attempts; i++) {
            // 每次尝试前先复原，基于同一起点进行随机探索
            if (i > 0) {
                Collections.copy(c.getMachineAssignment(), startAssignment);
                Collections.copy(c.getOperationSequence(), startSequence);
            }

            switch (k) {
                case 1: // Neighborhood 1: Reassign Machine (改变机器)
                    int idx = random.nextInt(size);
                    int newM = random.nextInt(ScheduleData.NUM_LINES) + 1;
                    c.getMachineAssignment().set(idx, newM);
                    break;

                case 2: // Neighborhood 2: Swap (交换工序)
                    int s1 = random.nextInt(size);
                    int s2 = random.nextInt(size);
                    Collections.swap(c.getOperationSequence(), s1, s2);
                    break;

                case 3: // Neighborhood 3: Insert (插入/移动工序)
                    // 把位置 from 的任务拿出来，插到 to 的位置
                    int from = random.nextInt(size);
                    int to = random.nextInt(size);
                    if (from != to) {
                        String op = c.getOperationSequence().remove(from);
                        c.getOperationSequence().add(to, op);
                        // 注意：机器分配数组也要同步移动，保持一一对应
                        Integer mach = c.getMachineAssignment().remove(from);
                        c.getMachineAssignment().add(to, mach);
                    }
                    break;
            }

            double newCost = decodeAndCalculateCost(c);
            if (newCost < currentCost) {
                currentCost = newCost;
                // 更新“本轮起点”，基于这个新解继续找，体现“爬山”特性
                startAssignment = new ArrayList<>(c.getMachineAssignment());
                startSequence = new ArrayList<>(c.getOperationSequence());
                improved = true;
                // 这里可以选择 break 立即返回，或者继续找更好的
            }
        }

        if (improved) {
            c.setFitness(currentCost);
            // 染色体已经是最好的状态了，无需 copy
        } else {
            // 没变好，完全复原
            Collections.copy(c.getMachineAssignment(), startAssignment);
            Collections.copy(c.getOperationSequence(), startSequence);
        }
        return improved;
    }

    /**
     * 核心调度解码器：将染色体转换为调度方案并计算成本
     * 实现了：
     * 1. 4小时工时块约束（每4小时换一次产品/结算一次工资）。
     * 2. 拼单逻辑：同类产品如果当前块有剩余时间，合并加工。
     * 3. 阶梯工资成本计算。
     */
//    private double decodeAndCalculateCost(Chromosome chromosome) {
//        // --- 1. 初始化状态 ---
//        Map<Integer, AtomicInteger> orderProgress = new HashMap<>();
//        data.getOrders().forEach(o -> orderProgress.put(o.getId(), new AtomicInteger(0)));
//
//        // 计划开始时间：2025-11-26 08:00
//        LocalDateTime planStartTime = LocalDateTime.of(2025, 11, 26, 8, 0);
//
//        // 生产线状态
//        // lineFreeTime: 生产线何时变为空闲（实际占用结束时间）
//        Map<Integer, LocalDateTime> lineFreeTime = new HashMap<>();
//        // linePaidUntil: 生产线当前已支付工资的时段结束时间（即当前4小时块的结束点）
//        Map<Integer, LocalDateTime> linePaidUntil = new HashMap<>();
//        // lineCurrentProduct: 生产线当前块正在生产的产品ID
//        Map<Integer, Integer> lineCurrentProduct = new HashMap<>();
//
//        for (int l = 1; l <= ScheduleData.NUM_LINES; l++) {
//            lineFreeTime.put(l, planStartTime);
//            linePaidUntil.put(l, planStartTime); // 初始时，已支付时间等于开始时间
//            lineCurrentProduct.put(l, -1); // -1 表示无产品
//        }
//
//        Map<Integer, LocalDateTime> orderCompletionTime = new HashMap<>();
//        double totalProductionCost = 0.0;
//
//        List<String> opSequence = chromosome.getOperationSequence();
//        List<Integer> assignment = chromosome.getMachineAssignment();
//
//        // --- 2. 遍历基因序列进行调度 ---
//        for (int i = 0; i < opSequence.size(); i++) {
//            String operationId = opSequence.get(i);
//            int machineLineId = assignment.get(i);
//
//            // 解析订单和产品信息
//            int orderId = Integer.parseInt(operationId.split("_")[0].substring(1));
//            Order currentOrder = data.getOrders().stream().filter(o -> o.getId() == orderId).findFirst().get();
//            int productId = currentOrder.getProductId();
//
//            // 获取该产品单个工件所需的加工时间（题目默认4小时，但为了支持"小于4小时合并"，这里动态获取）
//            Product product = data.getProducts().stream().filter(p -> p.getId() == productId).findFirst().get();
//            double processHours = product.getUnitProcessingTime();
//            long processSeconds = (long)(processHours * 3600);
//
//            LocalDateTime currentFree = lineFreeTime.get(machineLineId);
//            LocalDateTime currentPaidEnd = linePaidUntil.get(machineLineId);
//            int currentProd = lineCurrentProduct.get(machineLineId);
//
//            LocalDateTime taskStartTime;
//            LocalDateTime taskEndTime;
//
//            // --- 核心拼单逻辑 ---
//            // 判断：如果是同一种产品，且当前付费块(4小时)内还有足够剩余时间
//            boolean isSameProduct = (currentProd == productId);
//            // 计算当前块剩余秒数
//            long remainingSecondsInBlock = java.time.Duration.between(currentFree, currentPaidEnd).getSeconds();
//
//            if (isSameProduct && remainingSecondsInBlock >= processSeconds) {
//                // 【情况A：拼单】直接放入当前块，不产生新工资成本
//                taskStartTime = currentFree;
//                taskEndTime = taskStartTime.plusSeconds(processSeconds);
//                // 更新空闲时间，paidUntil不变
//                lineFreeTime.put(machineLineId, taskEndTime);
//            } else {
//                // 【情况B：新开工时块】
//                // 1. 如果当前块满了，或者要换产品，必须开启新的4小时块
//                // 2. 新块的开始时间必须对齐到4小时网格（8点, 12点, 16点...）且必须 >= currentFree
//
//                // 计算建议的开始时间：如果是换产品，最早也就是 currentFree；如果是同产品但满了，也是 currentFree
//                // 但根据规则“每4小时换一次产品”，通常意味着生产槽是固定的(8-12, 12-16)。
//                // 我们寻找下一个可用的4小时槽起点。
//
//                LocalDateTime slotStart = getNextSlotStartTime(planStartTime, currentFree);
//
//                // 开启新块
//                taskStartTime = slotStart;
//                taskEndTime = taskStartTime.plusSeconds(processSeconds);
//
//                // 计算该新块（4小时）的工资成本
//                double costCoeff = TimeCostUtil.getCostCoefficient(taskStartTime);
//                double blockCost = TimeCostUtil.BASE_PAY_4_HOURS * costCoeff;
//                totalProductionCost += blockCost;
//
//                // 更新状态
//                lineFreeTime.put(machineLineId, taskEndTime);
//                linePaidUntil.put(machineLineId, taskStartTime.plusHours(4)); // 支付了完整的4小时
//                lineCurrentProduct.put(machineLineId, productId);
//            }
//
//            // 记录订单完成时间
//            if (orderProgress.get(orderId).incrementAndGet() == currentOrder.getQuantity()) {
//                orderCompletionTime.put(orderId, taskEndTime);
//            }
//        }
//
//        // --- 3. 计算罚款 ---
//        double totalPenalty = 0.0;
//        for (Order order : data.getOrders()) {
//            LocalDateTime finishTime = orderCompletionTime.get(order.getId());
//            if (finishTime != null && finishTime.isAfter(order.getDeadline())) {
//                totalPenalty += order.getTotalValue() * ScheduleData.PENALTY_RATE;
//            }
//        }
//
//        return totalProductionCost + totalPenalty;
//    }

    /**
     * 辅助方法：获取基于计划开始时间对齐的下一个4小时槽的开始时间
     * 例如 planStart=8:00, current=10:00 -> 返回 12:00 (如果原槽不可用)
     * 这里简化逻辑：只要 currentFree 超过了上一个槽的结束，或者是新产品，就找最近的 >= currentFree 的槽起点
     */
    private LocalDateTime getNextSlotStartTime(LocalDateTime planStart, LocalDateTime currentFree) {
        // 计算从 planStart 开始经过了多少小时
        long hoursDiff = java.time.Duration.between(planStart, currentFree).toHours();
        // 找到所在的 4小时 block 索引
        long blockIndex = hoursDiff / 4;

        // 候选开始时间
        LocalDateTime candidate = planStart.plusHours(blockIndex * 4);

        // 如果候选时间在 currentFree 之前（说明在这个槽里已经过了一部分时间），
        // 既然我们进入这个方法说明“无法拼单”（要么换产品，要么时间不够），
        // 所以必须跳到下一个槽。
        if (candidate.isBefore(currentFree) || candidate.isEqual(currentFree)) {
            candidate = candidate.plusHours(4);
        }

        return candidate;
    }

    /**
     * 辅助函数：找到下一个 4 小时时间块的起始点 (8:00, 12:00, 16:00, 20:00, 0:00, 4:00)
     * 这是实现 4 小时换产品约束的关键。
     */
    private LocalDateTime getNextTimeBlockStart(LocalDateTime currentTime) {
        LocalDateTime nextTime = currentTime.plus(1, ChronoUnit.HOURS); // 从下一小时开始搜索，避免死循环

        while (true) {
            int hour = nextTime.getHour();
            // 检查是否是 4, 8, 12, 16, 20, 0 (24) 中的一个，并且分钟和秒为 0
            if ((hour % 4 == 0) && nextTime.getMinute() == 0 && nextTime.getSecond() == 0) {
                // 如果满足条件，检查它是否严格在当前时间之后
                if (nextTime.isAfter(currentTime)) {
                    return nextTime;
                }
            }
            nextTime = nextTime.plus(1, ChronoUnit.HOURS);
        }
    }

    /**
     * 选择：使用轮盘赌或锦标赛选择父代
     */
    private List<Chromosome> selection(List<Chromosome> population) {
        List<Chromosome> parents = new ArrayList<>(populationSize);
        int tournamentSize = 5; // 锦标赛规模 K=5

        for (int i = 0; i < populationSize; i++) {
            Chromosome bestInTournament = null;

            for (int j = 0; j < tournamentSize; j++) {
                // 随机选择一个个体
                Chromosome contender = population.get(random.nextInt(populationSize));

                // 选取适应度最好的 (Fitness值越小越好)
                if (bestInTournament == null || contender.getFitness() < bestInTournament.getFitness()) {
                    bestInTournament = contender;
                }
            }
            parents.add(bestInTournament);
        }
        return parents;
    }

    /**
     * 产生新一代：交叉和变异
     */
    private List<Chromosome> newGeneration(List<Chromosome> parents) {
        List<Chromosome> newPopulation = new ArrayList<>();

        // 确保精英保留 (Elitism): 将当前最佳解直接带入下一代
        Chromosome currentBest = parents.stream()
                .min((c1, c2) -> Double.compare(c1.getFitness(), c2.getFitness()))
                .orElse(parents.get(0)); // 至少保留一个解
        newPopulation.add(currentBest);

        // 循环进行交叉和变异，直到新种群大小达到要求
        for (int i = 0; i < populationSize - 1; i += 2) {
            Chromosome parent1 = parents.get(random.nextInt(parents.size()));
            Chromosome parent2 = parents.get(random.nextInt(parents.size()));

            Chromosome child1 = parent1;
            Chromosome child2 = parent2;

            if (random.nextDouble() < crossoverRate) {
                // 执行交叉操作
                List<Chromosome> children = crossover(parent1, parent2);
                child1 = children.get(0);
                child2 = children.get(1);
            }

            // 执行变异操作
            mutation(child1);
            if (newPopulation.size() < populationSize) {
                mutation(child2);
            }

            newPopulation.add(child1);
            if (newPopulation.size() < populationSize) {
                newPopulation.add(child2);
            }
        }
        return newPopulation.subList(0, Math.min(newPopulation.size(), populationSize));
    }

    /**
     * 交叉操作：对操作序列使用 PMX 或 OX，对机器分配使用均匀交叉。
     */
    private List<Chromosome> crossover(Chromosome p1, Chromosome p2) {
        List<Chromosome> children = new ArrayList<>(2);
        int len = p1.getOperationSequence().size();

        // 随机选择两个交叉点
        int c1 = random.nextInt(len);
        int c2 = random.nextInt(len);
        int start = Math.min(c1, c2);
        int end = Math.max(c1, c2);

        // --- 1. 操作序列交叉 (使用有序交叉 Order Crossover - OX) ---
        List<String> opSeq1 = orderCrossover(p1.getOperationSequence(), p2.getOperationSequence(), start, end);
        List<String> opSeq2 = orderCrossover(p2.getOperationSequence(), p1.getOperationSequence(), start, end);

        // --- 2. 机器分配交叉 (使用均匀交叉 Uniform Crossover) ---
        List<Integer> ma1 = uniformMachineCrossover(p1.getMachineAssignment(), p2.getMachineAssignment());
        List<Integer> ma2 = uniformMachineCrossover(p2.getMachineAssignment(), p1.getMachineAssignment());

        children.add(new Chromosome(opSeq1, ma1));
        children.add(new Chromosome(opSeq2, ma2));

        return children;
    }

    // 辅助方法：有序交叉 (Order Crossover, OX)
    private List<String> orderCrossover(List<String> p1Seq, List<String> p2Seq, int start, int end) {
        List<String> childSeq = new ArrayList<>(Collections.nCopies(p1Seq.size(), null));
        int len = p1Seq.size();

        // 1. 复制中间段
        for (int i = start; i <= end; i++) {
            childSeq.set(i, p1Seq.get(i));
        }

        // 2. 填充剩余部分
        int p2Index = (end + 1) % len;
        int childIndex = (end + 1) % len;

        // 循环直到 childSeq 被完全填充
        while (childSeq.contains(null)) {
            String gene = p2Seq.get(p2Index);
            if (!childSeq.contains(gene)) {
                childSeq.set(childIndex, gene);
                childIndex = (childIndex + 1) % len;
            }
            p2Index = (p2Index + 1) % len;
        }
        return childSeq;
    }

    // 辅助方法：均匀机器分配交叉 (Uniform Machine Assignment Crossover)
    private List<Integer> uniformMachineCrossover(List<Integer> ma1, List<Integer> ma2) {
        List<Integer> childMa = new ArrayList<>(ma1.size());
        for (int i = 0; i < ma1.size(); i++) {
            // 50% 概率继承 P1，50% 概率继承 P2
            if (random.nextBoolean()) {
                childMa.add(ma1.get(i));
            } else {
                childMa.add(ma2.get(i));
            }
        }
        return childMa;
    }

    /**
     * 变异操作：随机交换操作序列，随机重分配机器。
     */
    private void mutation(Chromosome chromosome) {

        // --- 1. 操作序列变异 (Swap Mutation) ---
        if (random.nextDouble() < mutationRate) {
            List<String> opSeq = chromosome.getOperationSequence();
            int len = opSeq.size();
            if (len > 1) {
                // 随机选择两个位置并交换它们
                int index1 = random.nextInt(len);
                int index2;
                do {
                    index2 = random.nextInt(len);
                } while (index1 == index2);

                Collections.swap(opSeq, index1, index2);
            }
        }

        // --- 2. 机器分配变异 (Random Assignment Mutation) ---
        if (random.nextDouble() < mutationRate) {
            List<Integer> assignment = chromosome.getMachineAssignment();
            int len = assignment.size();
            // 随机选择一个位置
            int index = random.nextInt(len);

            // 随机分配一个不同的生产线
            int newMachine;
            do {
                newMachine = random.nextInt(ScheduleData.NUM_LINES) + 1;
            } while (newMachine == assignment.get(index));

            assignment.set(index, newMachine);
        }
    }

    // 定义一个内部类或单独的类来封装解码结果
    public static class ScheduleResult {
        public double totalCost;
        public double totalPenalty; // [新增] 总罚款
        public List<Job> scheduledJobs;
        public Map<Integer, LocalDateTime> completionTimes; // [新增] 每个订单的完成时间

        public ScheduleResult(double cost, double penalty, List<Job> jobs, Map<Integer, LocalDateTime> completionTimes) {
            this.totalCost = cost;
            this.totalPenalty = penalty;
            this.scheduledJobs = jobs;
            this.completionTimes = completionTimes;
        }
    }

    /**
     * 获取最佳解的详细排程信息（对外暴露的接口）
     */
    public ScheduleResult getDetailedSchedule(Chromosome bestSolution) {
        return decode(bestSolution);
    }

    /**
     * 统一的解码函数：既计算Cost，也生成Job列表
     * 改进：增加尾数拼单优化（保持染色体结构一致性）
     */
    private ScheduleResult decode(Chromosome chromosome) {
        List<Job> jobs = new ArrayList<>();
        Map<Integer, AtomicInteger> orderProgress = new HashMap<>();
        data.getOrders().forEach(o -> orderProgress.put(o.getId(), new AtomicInteger(0)));

        // 如果订单列表为空，直接返回空结果
        if (data.getOrders().isEmpty()) {
            Map<Integer, LocalDateTime> emptyCompletionTimes = new HashMap<>();
            return new ScheduleResult(0.0, 0.0, jobs, emptyCompletionTimes);
        }

        LocalDateTime planStartTime = data.getPlanStartTime();

        // 初始化生产线状态
        Map<Integer, LocalDateTime> lineFreeTime = new HashMap<>();
        Map<Integer, LocalDateTime> linePaidUntil = new HashMap<>();
        Map<Integer, Integer> lineCurrentProduct = new HashMap<>();

        for (int l = 1; l <= ScheduleData.NUM_LINES; l++) {
            lineFreeTime.put(l, planStartTime);
            linePaidUntil.put(l, planStartTime);
            lineCurrentProduct.put(l, -1);
        }

        Map<Integer, LocalDateTime> orderCompletionTime = new HashMap<>();
        double totalProductionCost = 0.0;

        List<String> opSequence = chromosome.getOperationSequence();
        List<Integer> assignment = chromosome.getMachineAssignment();

        // ========== 预处理阶段：识别尾数工件 ==========
        Map<String, Boolean> isLastPieceOfOrder = new HashMap<>();
        Map<Integer, Integer> orderPieceCount = new HashMap<>();
        Map<Integer, Integer> orderTotalPieces = new HashMap<>();
        
        for (Order order : data.getOrders()) {
            orderTotalPieces.put(order.getId(), order.getQuantity());
        }
        
        // 标记每个工件是否是订单的最后一个
        for (int i = 0; i < opSequence.size(); i++) {
            String opId = opSequence.get(i);
            int orderId = Integer.parseInt(opId.split("_")[0].substring(1));
            int currentCount = orderPieceCount.getOrDefault(orderId, 0) + 1;
            orderPieceCount.put(orderId, currentCount);
            isLastPieceOfOrder.put(opId, currentCount == orderTotalPieces.get(orderId));
        }
        
        // 构建尾数工件索引列表（同一产品的尾数工件，且加工时间 < 4小时）
        Map<Integer, List<Integer>> tailPieceIndicesByProduct = new HashMap<>(); // 产品ID -> 尾数工件索引列表
        for (int i = 0; i < opSequence.size(); i++) {
            String opId = opSequence.get(i);
            if (isLastPieceOfOrder.getOrDefault(opId, false)) {
                int orderId = Integer.parseInt(opId.split("_")[0].substring(1));
                Order order = data.getOrders().stream()
                    .filter(o -> o.getId() == orderId).findFirst().get();
                int productId = order.getProductId();
                Product product = data.getProducts().stream()
                    .filter(p -> p.getId() == productId).findFirst().get();
                
                if (product.getUnitProcessingTime() < 4.0) {
                    tailPieceIndicesByProduct.computeIfAbsent(productId, k -> new ArrayList<>()).add(i);
                }
            }
        }
        
        // ========== 主调度循环 ==========
        // 记录已合并的尾数工件索引（避免重复合并）
        Set<Integer> mergedTailIndices = new HashSet<>();
        
        for (int i = 0; i < opSequence.size(); i++) {
            String opId = opSequence.get(i);
            int lineId = assignment.get(i);  // 使用染色体中指定的生产线
            
            // ========== 关键改进：不跳过任何工件 ==========
            // 即使这个工件已经被合并处理，也要为它生成Job
            // 只是Job的时间信息会与合并的主工件相同
            
            int orderId = Integer.parseInt(opId.split("_")[0].substring(1));
            Order order = data.getOrders().stream()
                .filter(o -> o.getId() == orderId).findFirst().get();
            int productId = order.getProductId();
            Product product = data.getProducts().stream()
                .filter(p -> p.getId() == productId).findFirst().get();
            double durationHours = product.getUnitProcessingTime();
            long durationSeconds = (long)(durationHours * 3600);

            LocalDateTime freeTime = lineFreeTime.get(lineId);
            LocalDateTime paidUntil = linePaidUntil.get(lineId);
            int currentProd = lineCurrentProduct.get(lineId);

            LocalDateTime startTime, endTime;
            double jobCost = 0.0;
            
            // ========== 主动尾数拼单逻辑 ==========
            boolean isTailPiece = isLastPieceOfOrder.getOrDefault(opId, false) && 
                                  durationHours < 4.0 &&
                                  !mergedTailIndices.contains(i);
            
            List<Integer> mergedIndices = new ArrayList<>(); // 记录合并的工件索引
            mergedIndices.add(i);
            double totalMergedHours = durationHours;
            
            if (isTailPiece) {
                // 尝试寻找其他可以合并的尾数工件（同一产品，且总时间 <= 4小时）
                List<Integer> availableTailIndices = tailPieceIndicesByProduct.getOrDefault(productId, new ArrayList<>());
                
                for (int otherIndex : availableTailIndices) {
                    if (otherIndex == i || mergedTailIndices.contains(otherIndex)) {
                        continue;
                    }
                    
                    // 关键：检查另一个工件是否也在同一生产线（使用染色体中的分配）
                    int otherLineId = assignment.get(otherIndex);
                    
                    // 如果另一个工件在同一生产线，可以合并
                    if (otherLineId == lineId) {
                        String otherOpId = opSequence.get(otherIndex);
                        int otherOrderId = Integer.parseInt(otherOpId.split("_")[0].substring(1));
                        Order otherOrder = data.getOrders().stream()
                            .filter(o -> o.getId() == otherOrderId).findFirst().get();
                        Product otherProduct = data.getProducts().stream()
                            .filter(p -> p.getId() == otherOrder.getProductId()).findFirst().get();
                        double otherDurationHours = otherProduct.getUnitProcessingTime();
                        
                        // 如果合并后总时间 <= 4小时，可以合并
                        if (totalMergedHours + otherDurationHours <= 4.0) {
                            mergedIndices.add(otherIndex);
                            totalMergedHours += otherDurationHours;
                            mergedTailIndices.add(otherIndex);
                        }
                    }
                }
                
                // 如果成功合并了多个尾数工件，使用合并后的总时间
                if (mergedIndices.size() > 1) {
                    durationSeconds = (long)(totalMergedHours * 3600);
                }
            }

            // --- 策略：拼单判断（原有逻辑）---
            long remainingSeconds = java.time.Duration.between(freeTime, paidUntil).getSeconds();

            if (currentProd == productId && remainingSeconds >= durationSeconds) {
                // [拼单模式]：插入当前块，无需额外付费
                startTime = freeTime;
                endTime = startTime.plusSeconds(durationSeconds);
                lineFreeTime.put(lineId, endTime);
                jobCost = 0.0;
            } else {
                // [新块模式]：开启新的4小时工时块
                LocalDateTime proposedStart = getNextGridTime(planStartTime, freeTime);
                startTime = proposedStart;
                endTime = startTime.plusSeconds(durationSeconds);

                // 计算该 4小时块 的成本
                double coeff = TimeCostUtil.getCostCoefficient(startTime);
                double baseCost = TimeCostUtil.BASE_PAY_4_HOURS * coeff;
                jobCost = baseCost;
                totalProductionCost += baseCost;

                // 更新状态
                lineFreeTime.put(lineId, endTime);
                linePaidUntil.put(lineId, startTime.plusHours(4));
                lineCurrentProduct.put(lineId, productId);
            }

            // ========== 关键改进：为所有合并的工件创建Job ==========
            // 每个工件都对应一个Job，保持染色体结构一致性
            for (int mergedIndex : mergedIndices) {
                String mergedOpId = opSequence.get(mergedIndex);
                int mergedOrderId = Integer.parseInt(mergedOpId.split("_")[0].substring(1));
                int mergedLineId = assignment.get(mergedIndex);  // 使用染色体中的分配
                
                // 只有第一个工件承担成本，其他工件成本为0（因为是合并的）
                double mergedJobCost = (mergedIndex == i) ? jobCost : 0.0;
                
                Job job = new Job(mergedOpId, productId, mergedLineId, startTime, endTime,
                        TimeCostUtil.getCostCoefficient(startTime), mergedJobCost);
                jobs.add(job);
                
                // 记录订单完成
                if (orderProgress.get(mergedOrderId).incrementAndGet() == 
                    data.getOrders().stream().filter(o -> o.getId() == mergedOrderId)
                        .findFirst().get().getQuantity()) {
                    orderCompletionTime.put(mergedOrderId, endTime);
                }
            }
            
            // 标记当前工件已处理（如果是尾数合并的一部分）
            if (mergedIndices.size() > 1) {
                mergedTailIndices.add(i);
            }
        }

        // 计算罚款
        // 根据作业条件：合同的截止时间均是指截止日期的当天早上8点
        double penalty = 0.0;
        for (Order o : data.getOrders()) {
            LocalDateTime finish = orderCompletionTime.get(o.getId());
            LocalDateTime alignedDeadline = o.getAlignedDeadline(); // 使用对齐后的截止时间
            if (finish != null && finish.isAfter(alignedDeadline)) {
                penalty += o.getTotalValue() * ScheduleData.PENALTY_RATE;
            }
        }

        // 计算总收入
        double totalRevenue = data.getOrders().stream()
                .mapToDouble(Order::getTotalValue)
                .sum();
        
        // 计算利润 = 总收入 - 生产成本 - 罚款
        double profit = totalRevenue - totalProductionCost - penalty;
        
        // 注意：适应度值越小越好，所以返回负利润作为"成本"
        return new ScheduleResult(-profit, penalty, jobs, orderCompletionTime);
    }

    // 复用之前的 decodeAndCalculateCost，改为调用统一的 decode
    private double decodeAndCalculateCost(Chromosome c) {
        return decode(c).totalCost;
    }

    /**
     * 辅助：找到基于 start 基准的下一个 4小时网格时间
     * 规则：如果 current 已经在网格线上，且之前的块已满或不可用，则返回 current；
     * 否则返回 current 之后的下一个网格线。
     * 简单实现：向上取整到 4小时倍数。
     */
    private LocalDateTime getNextGridTime(LocalDateTime base, LocalDateTime current) {
        long secondsFromBase = java.time.Duration.between(base, current).getSeconds();
        long blockSeconds = 4 * 3600;

        long blocksPassed = secondsFromBase / blockSeconds;
        long remainder = secondsFromBase % blockSeconds;

        if (remainder == 0) {
            return current; // 正好在开始点
        } else {
            return base.plusSeconds((blocksPassed + 1) * blockSeconds);
        }
    }
}
