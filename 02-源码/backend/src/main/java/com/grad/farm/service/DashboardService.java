package com.grad.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grad.farm.entity.*;
import com.grad.farm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * 运营看板：6 个核心指标 + 6 个图表。
 * 聚合全部在后端完成，前端只负责渲染，避免前端二次拉全表。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ServiceRequestMapper requestMapper;
    private final DispatchOrderMapper orderMapper;
    private final WorkRecordMapper workRecordMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final SettlementMapper settlementMapper;
    private final FarmMachineMapper machineMapper;
    private final OperatorMapper operatorMapper;
    private final FarmScoreMapper scoreMapper;

    public Map<String, Object> overview(String period) {
        LocalDate from = switch (period == null ? "month" : period) {
            case "quarter" -> LocalDate.now().minusMonths(3);
            case "year" -> LocalDate.now().withDayOfYear(1);
            case "30d" -> LocalDate.now().minusDays(30);
            default -> LocalDate.now().withDayOfMonth(1);
        };
        LocalDateTime start = from.atStartOfDay();

        List<WorkRecord> works = workRecordMapper.selectList(null);
        List<DispatchOrder> orders = orderMapper.selectList(null);
        List<ServiceRequest> requests = requestMapper.selectList(null);
        List<Acceptance> accepts = acceptanceMapper.selectList(null);
        List<Settlement> settles = settlementMapper.selectList(null);
        List<FarmMachine> machines = machineMapper.selectList(Wrappers.<FarmMachine>lambdaQuery()
                .eq(FarmMachine::getDeleted, 0));

        // 指标 1：作业面积（已完工记录累计）
        double totalArea = works.stream().filter(w -> "已完工".equals(w.getRecordStatus()))
                .map(w -> w.getActualArea() == null ? 0 : w.getActualArea().doubleValue())
                .mapToDouble(Double::doubleValue).sum();

        // 指标 2：机具利用率 = 本期内有派单的机具 / 可用机具总数
        Set<Long> usedMachine = new HashSet<>();
        for (DispatchOrder o : orders) {
            if (o.getPlanStart() != null && o.getPlanStart().isAfter(start)) usedMachine.add(o.getMachineId());
        }
        double utilization = machines.isEmpty() ? 0 : usedMachine.size() * 100.0 / machines.size();

        // 指标 3：按时完工率（完工时间不晚于计划结束时间）
        long finished = 0, ontime = 0;
        for (DispatchOrder o : orders) {
            if (o.getFinishTime() != null && o.getPlanEnd() != null) {
                finished++;
                if (!o.getFinishTime().isAfter(o.getPlanEnd())) ontime++;
            }
        }
        double ontimeRate = finished == 0 ? 0 : ontime * 100.0 / finished;

        // 指标 4：平均亩均油耗
        double fuelSum = 0, areaSum = 0;
        for (WorkRecord w : works) {
            if (w.getFuelLiters() != null && w.getActualArea() != null && w.getActualArea().doubleValue() > 0) {
                fuelSum += w.getFuelLiters().doubleValue();
                areaSum += w.getActualArea().doubleValue();
            }
        }
        double avgFuel = areaSum == 0 ? 0 : fuelSum / areaSum;

        // 指标 5：返工率
        long rework = accepts.stream().filter(a -> a.getReworkFlag() != null && a.getReworkFlag() == 1).count();
        double reworkRate = accepts.isEmpty() ? 0 : rework * 100.0 / accepts.size();

        // 指标 6：应收完成率
        double receivable = settles.stream().map(s -> s.getReceivable() == null ? 0 : s.getReceivable().doubleValue())
                .mapToDouble(Double::doubleValue).sum();
        double received = settles.stream().map(s -> s.getReceived() == null ? 0 : s.getReceived().doubleValue())
                .mapToDouble(Double::doubleValue).sum();
        double receiveRate = receivable == 0 ? 0 : received * 100.0 / receivable;

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalArea", round(totalArea, 1));
        kpi.put("utilization", round(utilization, 1));
        kpi.put("ontimeRate", round(ontimeRate, 1));
        kpi.put("avgFuel", round(avgFuel, 2));
        kpi.put("reworkRate", round(reworkRate, 1));
        kpi.put("receiveRate", round(receiveRate, 1));
        kpi.put("requestCount", requests.size());
        kpi.put("orderCount", orders.size());
        kpi.put("receivable", round(receivable, 2));
        kpi.put("unreceived", round(receivable - received, 2));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("kpi", kpi);

        // 图 1：近 6 个月作业面积趋势
        List<String> months = new ArrayList<>();
        List<Double> monthArea = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            months.add(ym.getMonthValue() + "月");
            double area = 0;
            for (WorkRecord w : works) {
                if (w.getStartTime() == null || w.getActualArea() == null) continue;
                if (YearMonth.from(w.getStartTime()).equals(ym)) area += w.getActualArea().doubleValue();
            }
            monthArea.add(round(area, 1));
        }
        res.put("trend", Map.of("months", months, "area", monthArea));

        // 图 2：服务类型面积分布
        Map<String, Double> byType = new LinkedHashMap<>();
        for (DispatchOrder o : orders) {
            ServiceRequest r = requests.stream().filter(x -> x.getId().equals(o.getRequestId())).findFirst().orElse(null);
            if (r == null) continue;
            WorkRecord w = works.stream().filter(x -> o.getId().equals(x.getOrderId())).findFirst().orElse(null);
            double a = w != null && w.getActualArea() != null ? w.getActualArea().doubleValue()
                    : (o.getPlanArea() == null ? 0 : o.getPlanArea().doubleValue());
            byType.merge(r.getServiceType(), a, Double::sum);
        }
        res.put("serviceTypes", byType.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", round(e.getValue(), 1))).toList());

        // 图 3：订单状态分布
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DispatchOrder o : orders) byStatus.merge(o.getDispatchStatus(), 1L, Long::sum);
        res.put("orderStatus", byStatus.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 图 4：机具利用率 TOP8（按派单数）
        Map<String, Long> byMachine = new LinkedHashMap<>();
        for (DispatchOrder o : orders) byMachine.merge(o.getMachineNo(), 1L, Long::sum);
        res.put("machineRank", byMachine.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(8)
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 图 5：农机手工时 TOP6
        Map<String, Double> byOperator = new LinkedHashMap<>();
        for (WorkRecord w : works) {
            if (w.getWorkHours() == null) continue;
            DispatchOrder o = orders.stream().filter(x -> x.getId().equals(w.getOrderId())).findFirst().orElse(null);
            if (o == null) continue;
            byOperator.merge(o.getOperatorName(), w.getWorkHours().doubleValue(), Double::sum);
        }
        res.put("operatorRank", byOperator.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).limit(6)
                .map(e -> Map.of("name", e.getKey(), "value", round(e.getValue(), 1))).toList());

        // 图 6：创新推荐结果分布
        List<FarmScore> scores = scoreMapper.selectList(null);
        Map<String, Long> byLevel = new LinkedHashMap<>();
        byLevel.put("推荐", 0L);
        byLevel.put("可选", 0L);
        byLevel.put("不推荐", 0L);
        for (FarmScore s : scores) byLevel.merge(s.getResultLevel(), 1L, Long::sum);
        res.put("scoreLevel", byLevel.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue())).toList());

        // 待办提醒
        long waitDispatch = requests.stream().filter(r -> "待排程".equals(r.getRequestStatus())).count();
        long waitAccept = orders.stream().filter(o -> "待验收".equals(o.getDispatchStatus())).count();
        long waitPay = settles.stream().filter(s -> !"已收款".equals(s.getSettleStatus())).count();
        res.put("todo", Map.of("waitDispatch", waitDispatch, "waitAccept", waitAccept, "waitPay", waitPay));
        return res;
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
