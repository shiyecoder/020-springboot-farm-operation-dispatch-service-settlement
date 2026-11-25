package com.grad.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.farm.common.BizException;
import com.grad.farm.common.PageResult;
import com.grad.farm.common.Status;
import com.grad.farm.common.UserContext;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.*;
import com.grad.farm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 排程派单 + 创新点「机具能力与成本联合派单」。
 *
 * 计算口径（6 个因子，权重合计 100%）：
 *  1. 能力匹配 25%：机具可作业类型是否覆盖需求的服务类型；
 *  2. 资质有效期 15%：农农机手操作证在希望作业日期前的剩余有效期分档；
 *  3. 空闲时段 20%：机具状态可用性 + 与既有派单时段是否冲突；
 *  4. 到场距离 15%：地块里程 + 跨片区补偿；
 *  5. 历史油耗 10%：机具亩均油耗分档，越低越好；
 *  6. 准时率 15%：农机手历史准时率分档。
 *
 * 总分 = Σ(单项得分 × 权重) / 100，规则版本、输入快照、各项得分全部落库，保证可追溯。
 * 人工调整只写 final_level 与原因，原始 result_level 不被覆盖。
 */
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final FarmMachineMapper machineMapper;
    private final OperatorMapper operatorMapper;
    private final ServiceRequestMapper requestMapper;
    private final QuotationMapper quotationMapper;
    private final DispatchOrderMapper orderMapper;
    private final WorkRecordMapper workRecordMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final LandMapper landMapper;
    private final FarmRuleMapper ruleMapper;
    private final FarmScoreMapper scoreMapper;
    private final FarmRecordMapper recordMapper;
    private final SysMessageMapper messageMapper;
    private final ObjectMapper json;

    /* ==================== 规则 ==================== */

    public FarmRule activeRule() {
        FarmRule r = ruleMapper.selectOne(Wrappers.<FarmRule>lambdaQuery()
                .eq(FarmRule::getRuleStatus, "启用").orderByDesc(FarmRule::getId).last("limit 1"));
        if (r == null) throw new BizException("未配置启用的派单规则，请先在规则配置页维护");
        return r;
    }

    /* ==================== 推荐计算 ==================== */

    /**
     * 试算：返回候选机具 + 农机手组合的 6 项得分，不落库（用于排程台实时预览）。
     */
    public Map<String, Object> candidates(Long requestId) {
        ServiceRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException(404, "需求不存在");
        Land land = landMapper.selectById(req.getLandId());
        FarmRule rule = activeRule();
        List<Map<String, Object>> rows = compute(req, land, rule, false);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("request", req);
        res.put("rule", rule);
        res.put("candidates", rows);
        return res;
    }

    /** 落库计算：保存输入快照与明细得分，返回带 scoreId 的结果 */
    @Transactional
    public Map<String, Object> evaluate(Requests.EvaluateReq req) {
        ServiceRequest r = requestMapper.selectById(req.getRequestId());
        if (r == null) throw new BizException(404, "需求不存在");
        Land land = landMapper.selectById(r.getLandId());
        FarmRule rule = req.getRuleId() == null ? activeRule() : ruleMapper.selectById(req.getRuleId());
        if (rule == null) throw new BizException(404, "规则不存在");
        List<Map<String, Object>> rows = compute(r, land, rule, true);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("request", r);
        res.put("rule", rule);
        res.put("candidates", rows);
        return res;
    }

    private List<Map<String, Object>> compute(ServiceRequest req, Land land, FarmRule rule, boolean persist) {
        double wCap = d(rule.getWCapability()), wCert = d(rule.getWCert()), wFree = d(rule.getWFree());
        double wDist = d(rule.getWDistance()), wFuel = d(rule.getWFuel()), wOn = d(rule.getWOntime());
        double sum = wCap + wCert + wFree + wDist + wFuel + wOn;
        if (Math.abs(sum - 100) > 0.01) {
            throw new BizException("规则权重合计必须等于 100%，当前为 " + sum + "%");
        }
        double high = d(rule.getLevelHigh()), mid = d(rule.getLevelMid());

        // 候选机具：可作业类型覆盖需求类型，且未删除
        List<FarmMachine> machines = machineMapper.selectList(Wrappers.<FarmMachine>lambdaQuery()
                .eq(FarmMachine::getDeleted, 0).like(FarmMachine::getWorkTypes, req.getServiceType())
                .orderByAsc(FarmMachine::getMachineNo));
        // 兜底：若价目/机型未配置，用全部可用机具参与，能力匹配项自然给低分
        if (machines.isEmpty()) {
            machines = machineMapper.selectList(Wrappers.<FarmMachine>lambdaQuery()
                    .eq(FarmMachine::getDeleted, 0).orderByAsc(FarmMachine::getMachineNo));
        }
        List<Operator> operators = operatorMapper.selectList(Wrappers.<Operator>lambdaQuery()
                .eq(Operator::getDeleted, 0).ne(Operator::getCurrentStatus, "停用"));
        LocalDate hope = req.getHopeDate() == null ? LocalDate.now() : req.getHopeDate();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (FarmMachine m : machines) {
            Operator op = pickOperator(m, operators, req);
            double cap = capScore(m, req.getServiceType());
            double cert = certScore(op, hope);
            double free = freeScore(m, req);
            double dist = distScore(land, m);
            double fuel = fuelScore(m);
            double ontime = ontimeScore(op);
            double total = (cap * wCap + cert * wCert + free * wFree + dist * wDist + fuel * wFuel + ontime * wOn) / 100.0;
            total = round(total);
            String level = Status.levelOf(total, high, mid);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("machineId", m.getId());
            row.put("machineNo", m.getMachineNo());
            row.put("machineName", m.getMachineName());
            row.put("machineType", m.getMachineType());
            row.put("machineStatus", m.getCurrentStatus());
            row.put("coverImage", m.getCoverImage());
            row.put("operatorId", op == null ? null : op.getId());
            row.put("operatorName", op == null ? "—" : op.getRealName());
            row.put("operatorPhone", op == null ? null : op.getPhone());
            row.put("scores", List.of(
                    Map.of("key", "capability", "name", "能力匹配", "weight", wCap, "score", round(cap)),
                    Map.of("key", "cert", "name", "资质有效期", "weight", wCert, "score", round(cert)),
                    Map.of("key", "free", "name", "空闲时段", "weight", wFree, "score", round(free)),
                    Map.of("key", "distance", "name", "到场距离", "weight", wDist, "score", round(dist)),
                    Map.of("key", "fuel", "name", "历史油耗", "weight", wFuel, "score", round(fuel)),
                    Map.of("key", "ontime", "name", "准时率", "weight", wOn, "score", round(ontime))));
            row.put("totalScore", total);
            row.put("level", level);
            row.put("explain", explain(cap, cert, free, dist, fuel, ontime, wCap, wCert, wFree, wDist, wFuel, wOn));
            row.put("unmet", unmet(cap, cert, free, m, op, req));
            row.put("improve", improve(dist, free, fuel, ontime));
            row.put("certExpire", op == null ? null : op.getCertExpire());
            row.put("ontimeRate", op == null ? null : op.getOntimeRate());

            if (persist) {
                FarmScore s = new FarmScore();
                s.setRuleId(rule.getId());
                s.setRuleVersion(rule.getRuleVersion());
                s.setRequestId(req.getId());
                s.setRequestNo(req.getRequestNo());
                s.setMachineId(m.getId());
                s.setMachineNo(m.getMachineNo());
                s.setOperatorId(op == null ? null : op.getId());
                s.setOperatorName(op == null ? null : op.getRealName());
                s.setCapabilityScore(bd(cap));
                s.setCertScore(bd(cert));
                s.setFreeScore(bd(free));
                s.setDistanceScore(bd(dist));
                s.setFuelScore(bd(fuel));
                s.setOntimeScore(bd(ontime));
                s.setTotalScore(bd(total));
                s.setResultLevel(level);
                s.setFinalLevel(level);
                s.setExplainText(String.valueOf(row.get("explain")));
                s.setUnmetText(String.valueOf(row.get("unmet")));
                s.setImproveText(String.valueOf(row.get("improve")));
                s.setSnapshot(snapshot(req, land, m, op, rule));
                scoreMapper.insert(s);
                row.put("scoreId", s.getId());
            }
            rows.add(row);
        }
        rows.sort((a, b) -> Double.compare((Double) b.get("totalScore"), (Double) a.get("totalScore")));
        return rows;
    }

    private Operator pickOperator(FarmMachine m, List<Operator> operators, ServiceRequest req) {
        Operator best = null;
        for (Operator o : operators) {
            if (o.getCertTypes() == null) continue;
            boolean can = Arrays.stream(o.getCertTypes().split(","))
                    .map(String::trim).anyMatch(t -> t.equals(m.getMachineType()));
            if (!can) continue;
            if (best == null) best = o;
            else if (d(o.getOntimeRate()) > d(best.getOntimeRate())) best = o;
        }
        if (best == null && !operators.isEmpty()) best = operators.get(0);
        return best;
    }

    /* ---- 单项得分 ---- */

    private double capScore(FarmMachine m, String serviceType) {
        String types = m.getWorkTypes() == null ? "" : m.getWorkTypes();
        if (Arrays.stream(types.split(",")).map(String::trim).anyMatch(serviceType::equals)) return 100;
        if ("拖拉机".equals(m.getMachineType())
                && List.of("深耕", "旋耕", "播种").contains(serviceType)) return 58;
        return 25;
    }

    private double certScore(Operator o, LocalDate hope) {
        if (o == null || o.getCertExpire() == null) return 30;
        long days = ChronoUnit.DAYS.between(hope, o.getCertExpire());
        if (days > 180) return 100;
        if (days > 90) return 90;
        if (days > 30) return 76;
        if (days >= 0) return 55;
        return 20;
    }

    private double freeScore(FarmMachine m, ServiceRequest req) {
        if (Status.M_DISABLED.equals(m.getCurrentStatus())) return 0;
        if (Status.M_MAINTAIN.equals(m.getCurrentStatus()) || Status.M_REPAIR.equals(m.getCurrentStatus())) return 42;
        LocalDateTime start = req.getHopeDate() == null ? LocalDateTime.now()
                : req.getHopeDate().atTime(8, 0);
        LocalDateTime end = start.plusHours(10);
        Long overlap = orderMapper.selectCount(Wrappers.<DispatchOrder>lambdaQuery()
                .eq(DispatchOrder::getMachineId, m.getId())
                .in(DispatchOrder::getDispatchStatus, List.of(Status.WAIT_WORK, Status.WORKING, Status.WAIT_ACCEPT))
                .lt(DispatchOrder::getPlanStart, end)
                .gt(DispatchOrder::getPlanEnd, start));
        return overlap != null && overlap > 0 ? 62 : 100;
    }

    private double distScore(Land land, FarmMachine m) {
        double km = land == null || land.getDistanceKm() == null ? 0 : land.getDistanceKm().doubleValue();
        if (land != null && m.getRegion() != null && !m.getRegion().equals(land.getRegion())) km += 6;
        if (km <= 5) return 100;
        if (km <= 10) return 88;
        if (km <= 20) return 74;
        if (km <= 40) return 58;
        return 40;
    }

    private double fuelScore(FarmMachine m) {
        double f = m.getFuelPerMu() == null ? 2.5 : m.getFuelPerMu().doubleValue();
        if (f <= 1.0) return 100;
        if (f <= 1.6) return 92;
        if (f <= 2.0) return 84;
        if (f <= 2.4) return 72;
        return 62;
    }

    private double ontimeScore(Operator o) {
        if (o == null) return 50;
        double v = d(o.getOntimeRate());
        if (v >= 97) return 100;
        if (v >= 95) return 95;
        if (v >= 92) return 90;
        if (v >= 88) return 80;
        if (v >= 85) return 70;
        return 60;
    }

    private String explain(double cap, double cert, double free, double dist, double fuel, double on,
                           double wCap, double wCert, double wFree, double wDist, double wFuel, double wOn) {
        double[] s = {cap, cert, free, dist, fuel, on};
        double[] w = {wCap, wCert, wFree, wDist, wFuel, wOn};
        String[] names = {"能力匹配", "资质有效期", "空闲时段", "到场距离", "历史油耗", "准时率"};
        Integer[] idx = {0, 1, 2, 3, 4, 5};
        Arrays.sort(idx, (a, b) -> Double.compare(s[b] * w[b], s[a] * w[a]));
        return String.format("%s与%s贡献最大（各得 %.0f 分，权重 %.0f%%/%.0f%%），合计贡献 %.1f 分；"
                        + "%s是当前主要短板（%.0f 分）。",
                names[idx[0]], names[idx[1]], s[idx[0]], w[idx[0]], w[idx[1]],
                (s[idx[0]] * w[idx[0]] + s[idx[1]] * w[idx[1]]) / 100,
                names[idx[5]], s[idx[5]]);
    }

    private String unmet(double cap, double cert, double free, FarmMachine m, Operator op, ServiceRequest req) {
        List<String> list = new ArrayList<>();
        if (cert < 60) list.add("农机手操作证在希望作业日期前失效");
        if (free <= 45) list.add("机具当前处于「" + m.getCurrentStatus() + "」，不可派单");
        if (free > 45 && free < 100) list.add("计划时段与该机具既有派单重叠");
        if (cap < 60) list.add("机具可作业类型不含「" + req.getServiceType() + "」");
        if (op == null) list.add("没有匹配机型的农机手");
        return list.isEmpty() ? "无" : String.join("；", list);
    }

    private String improve(double dist, double free, double fuel, double ontime) {
        StringBuilder sb = new StringBuilder();
        if (free < 100) sb.append("调整计划时段或改派其他机具可消除时段冲突；");
        if (dist < 88) sb.append("就近驻点可缩短空驶里程，距离得分可提升至 88 以上；");
        if (fuel < 92) sb.append("优化作业档位与幅宽可降低亩均油耗；");
        if (ontime < 95) sb.append("加强到场时间管理可提升准时率得分。");
        return sb.length() == 0 ? "各因子已接近满分，无显著改进空间" : sb.toString();
    }

    private String snapshot(ServiceRequest r, Land land, FarmMachine m, Operator op, FarmRule rule) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("serviceType", r.getServiceType());
        s.put("crop", r.getCrop());
        s.put("area", r.getArea());
        s.put("hopeDate", String.valueOf(r.getHopeDate()));
        s.put("urgency", r.getUrgency());
        s.put("distanceKm", land == null ? null : land.getDistanceKm());
        s.put("landRegion", land == null ? null : land.getRegion());
        s.put("machineNo", m.getMachineNo());
        s.put("machineType", m.getMachineType());
        s.put("machineStatus", m.getCurrentStatus());
        s.put("fuelPerMu", m.getFuelPerMu());
        s.put("operatorName", op == null ? null : op.getRealName());
        s.put("certExpire", op == null ? null : String.valueOf(op.getCertExpire()));
        s.put("ontimeRate", op == null ? null : op.getOntimeRate());
        s.put("ruleVersion", rule.getRuleVersion());
        s.put("weights", List.of(rule.getWCapability(), rule.getWCert(), rule.getWFree(),
                rule.getWDistance(), rule.getWFuel(), rule.getWOntime()));
        try {
            return json.writeValueAsString(s);
        } catch (Exception e) {
            return "{}";
        }
    }

    /* ==================== 派单 ==================== */

    @Transactional
    public DispatchOrder dispatch(Requests.DispatchReq req) {
        ServiceRequest r = requestMapper.selectById(req.getRequestId());
        if (r == null) throw new BizException(404, "需求不存在");
        if (!Status.SCHEDULING.equals(r.getRequestStatus())) {
            throw new BizException("只有「待排程」的需求可以派单，当前为「" + r.getRequestStatus() + "」");
        }
        FarmMachine m = machineMapper.selectById(req.getMachineId());
        if (m == null) throw new BizException(404, "机具不存在");
        Operator op = operatorMapper.selectById(req.getOperatorId());
        if (op == null) throw new BizException(404, "农机手不存在");
        if (!Status.machineDispatchable(m.getCurrentStatus())) {
            throw new BizException("机具「" + m.getMachineNo() + "」当前为「" + m.getCurrentStatus() + "」，不可派单");
        }
        // 能力校验：机具可作业类型必须覆盖服务类型
        String types = m.getWorkTypes() == null ? "" : m.getWorkTypes();
        if (Arrays.stream(types.split(",")).map(String::trim).noneMatch(r.getServiceType()::equals)) {
            throw new BizException("机具「" + m.getMachineNo() + "」不支持「" + r.getServiceType() + "」作业");
        }
        LocalDateTime ps = req.getPlanStart() == null ? LocalDateTime.now() : req.getPlanStart();
        LocalDateTime pe = req.getPlanEnd() == null ? ps.plusHours(8) : req.getPlanEnd();
        if (!pe.isAfter(ps)) throw new BizException("计划结束时间必须晚于开始时间");
        // 资质校验：操作证在计划作业日必须有效
        if (op.getCertExpire() == null) throw new BizException("农机手「" + op.getRealName() + "」未登记操作证有效期");
        if (op.getCertExpire().isBefore(ps.toLocalDate())) {
            throw new BizException("农机手「" + op.getRealName() + "」操作证已于 " + op.getCertExpire() + " 到期，不可派单");
        }
        if (!"在岗".equals(op.getCurrentStatus())) {
            throw new BizException("农机手「" + op.getRealName() + "」当前为「" + op.getCurrentStatus() + "」，不可派单");
        }
        // 时段冲突校验：机具与农机手均不得重叠
        assertNoOverlap("机具", m.getMachineNo(), orderMapper.selectCount(Wrappers.<DispatchOrder>lambdaQuery()
                .eq(DispatchOrder::getMachineId, m.getId())
                .in(DispatchOrder::getDispatchStatus, List.of(Status.WAIT_WORK, Status.WORKING, Status.WAIT_ACCEPT))
                .lt(DispatchOrder::getPlanStart, pe).gt(DispatchOrder::getPlanEnd, ps)));
        assertNoOverlap("农机手", op.getRealName(), orderMapper.selectCount(Wrappers.<DispatchOrder>lambdaQuery()
                .eq(DispatchOrder::getOperatorId, op.getId())
                .in(DispatchOrder::getDispatchStatus, List.of(Status.WAIT_WORK, Status.WORKING, Status.WAIT_ACCEPT))
                .lt(DispatchOrder::getPlanStart, pe).gt(DispatchOrder::getPlanEnd, ps)));

        Quotation qt = quotationMapper.selectOne(Wrappers.<Quotation>lambdaQuery()
                .eq(Quotation::getRequestId, r.getId()).eq(Quotation::getQuoteStatus, "已确认")
                .orderByDesc(Quotation::getId).last("limit 1"));

        DispatchOrder o = new DispatchOrder();
        o.setOrderNo("PD" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (orderMapper.selectCount(null) + 1)));
        o.setRequestId(r.getId());
        o.setRequestNo(r.getRequestNo());
        o.setQuotationId(qt == null ? null : qt.getId());
        o.setMachineId(m.getId());
        o.setMachineNo(m.getMachineNo());
        o.setOperatorId(op.getId());
        o.setOperatorName(op.getRealName());
        o.setPlanStart(ps);
        o.setPlanEnd(pe);
        o.setPlanArea(r.getArea());
        o.setDispatchStatus(Status.WAIT_WORK);
        o.setScoreId(req.getScoreId());
        o.setDispatcherId(UserContext.userId());
        o.setDispatchTime(LocalDateTime.now());
        o.setUrgentFlag("普通".equals(r.getUrgency()) ? 0 : 1);
        o.setRemark(req.getRemark());
        orderMapper.insert(o);

        r.setRequestStatus(Status.WAIT_WORK);
        requestMapper.updateById(r);

        FarmRecord rec = new FarmRecord();
        rec.setBizType("ORDER");
        rec.setBizId(o.getId());
        rec.setBizNo(o.getOrderNo());
        rec.setAction("派单");
        rec.setFromStatus(Status.SCHEDULING);
        rec.setToStatus(Status.WAIT_WORK);
        rec.setOperatorName(UserContext.realName());
        rec.setReason("机具 " + m.getMachineNo() + " / 农机手 " + op.getRealName()
                + (req.getScoreId() == null ? "" : "（依据推荐记录 #" + req.getScoreId() + "）"));
        recordMapper.insert(rec);

        SysMessage msg = new SysMessage();
        msg.setReceiverId(op.getUserId() == null ? 6L : op.getUserId());
        msg.setTitle("新派单 " + o.getOrderNo());
        msg.setContent("需求 " + r.getRequestNo() + "：" + r.getServiceType() + " " + r.getArea()
                + " 亩，计划 " + ps.toLocalDate() + " 开工");
        msg.setBizNo(o.getOrderNo());
        msg.setBizType("ORDER");
        msg.setUrl("/work");
        msg.setIsRead(0);
        messageMapper.insert(msg);
        return o;
    }

    private void assertNoOverlap(String what, String name, Long count) {
        if (count != null && count > 0) {
            throw new BizException(what + "「" + name + "」在所选时段已有未完成派单，请调整时间或改派");
        }
    }

    /* ==================== 派单列表 / 详情 ==================== */

    public PageResult<Map<String, Object>> orderPage(Map<String, String> q) {
        long page = RequestService.num(q.get("page"), 1), size = RequestService.num(q.get("size"), 10);
        QueryWrapper<DispatchOrder> w = new QueryWrapper<>();
        if ("SELF".equals(UserContext.dataScope()) && "OPERATOR".equals(UserContext.roleCode())) {
            w.eq("operator_id", operatorIdOfCurrentUser());
        }
        if (notBlank(q.get("keyword"))) {
            w.and(x -> x.like("order_no", q.get("keyword")).or().like("request_no", q.get("keyword"))
                    .or().like("operator_name", q.get("keyword")).or().like("machine_no", q.get("keyword")));
        }
        if (notBlank(q.get("status"))) w.eq("dispatch_status", q.get("status"));
        if (notBlank(q.get("machineId"))) w.eq("machine_id", q.get("machineId"));
        if (notBlank(q.get("operatorId"))) w.eq("operator_id", q.get("operatorId"));
        if (notBlank(q.get("startDate"))) w.ge("plan_start", q.get("startDate") + " 00:00:00");
        if (notBlank(q.get("endDate"))) w.le("plan_start", q.get("endDate") + " 23:59:59");
        w.orderByDesc("id");
        Page<DispatchOrder> p = orderMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DispatchOrder o : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", o.getId());
            x.put("orderNo", o.getOrderNo());
            x.put("requestNo", o.getRequestNo());
            x.put("machineNo", o.getMachineNo());
            x.put("operatorName", o.getOperatorName());
            x.put("planStart", o.getPlanStart());
            x.put("planEnd", o.getPlanEnd());
            x.put("planArea", o.getPlanArea());
            x.put("status", o.getDispatchStatus());
            x.put("urgentFlag", o.getUrgentFlag());
            x.put("dispatchTime", o.getDispatchTime());
            x.put("finishTime", o.getFinishTime());
            ServiceRequest req = requestMapper.selectById(o.getRequestId());
            if (req != null) {
                x.put("farmerName", req.getFarmerName());
                x.put("landName", req.getLandName());
                x.put("serviceType", req.getServiceType());
                x.put("crop", req.getCrop());
                x.put("area", req.getArea());
                x.put("hopeDate", req.getHopeDate());
            }
            WorkRecord wr = workRecordMapper.selectOne(Wrappers.<WorkRecord>lambdaQuery()
                    .eq(WorkRecord::getOrderId, o.getId()).orderByDesc(WorkRecord::getId).last("limit 1"));
            x.put("actualArea", wr == null ? null : wr.getActualArea());
            x.put("fuelLiters", wr == null ? null : wr.getFuelLiters());
            x.put("workHours", wr == null ? null : wr.getWorkHours());
            x.put("beforeImage", wr == null ? null : wr.getBeforeImage());
            x.put("afterImage", wr == null ? null : wr.getAfterImage());
            Acceptance ac = acceptanceMapper.selectOne(Wrappers.<Acceptance>lambdaQuery()
                    .eq(Acceptance::getOrderId, o.getId()).orderByDesc(Acceptance::getId).last("limit 1"));
            x.put("acceptTime", ac == null ? null : ac.getAcceptTime());
            x.put("qualityScore", ac == null ? null : ac.getQualityScore());
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> orderDetail(Long id) {
        DispatchOrder o = orderMapper.selectById(id);
        if (o == null) throw new BizException(404, "派单不存在");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("order", o);
        ServiceRequest r = requestMapper.selectById(o.getRequestId());
        res.put("request", r);
        res.put("land", r == null ? null : landMapper.selectById(r.getLandId()));
        res.put("machine", machineMapper.selectById(o.getMachineId()));
        res.put("operator", operatorMapper.selectById(o.getOperatorId()));
        res.put("quotation", o.getQuotationId() == null ? null : quotationMapper.selectById(o.getQuotationId()));
        res.put("workRecords", workRecordMapper.selectList(Wrappers.<WorkRecord>lambdaQuery()
                .eq(WorkRecord::getOrderId, id).orderByDesc(WorkRecord::getId)));
        res.put("acceptance", acceptanceMapper.selectOne(Wrappers.<Acceptance>lambdaQuery()
                .eq(Acceptance::getOrderId, id).orderByDesc(Acceptance::getId).last("limit 1")));
        res.put("records", recordMapper.selectList(Wrappers.<FarmRecord>lambdaQuery()
                .eq(FarmRecord::getBizType, "ORDER").eq(FarmRecord::getBizId, id).orderByAsc(FarmRecord::getId)));
        if (o.getScoreId() != null) {
            res.put("score", scoreMapper.selectById(o.getScoreId()));
        }
        return res;
    }

    /** 取消派单：仅「待作业」可取消，取消后需求回到待排程 */
    @Transactional
    public void cancelOrder(Long id, String reason) {
        DispatchOrder o = orderMapper.selectById(id);
        if (o == null) throw new BizException(404, "派单不存在");
        if (!Status.WAIT_WORK.equals(o.getDispatchStatus())) {
            throw new BizException("只有「待作业」的派单可以取消，当前为「" + o.getDispatchStatus() + "」");
        }
        if (reason == null || reason.trim().length() < 5) throw new BizException("取消原因不少于 5 个字");
        o.setDispatchStatus("已取消");
        o.setRemark(reason.trim());
        orderMapper.updateById(o);
        ServiceRequest r = requestMapper.selectById(o.getRequestId());
        if (r != null) {
            r.setRequestStatus(Status.SCHEDULING);
            r.setBackReason("派单取消：" + reason.trim());
            requestMapper.updateById(r);
        }
        FarmRecord rec = new FarmRecord();
        rec.setBizType("ORDER");
        rec.setBizId(id);
        rec.setBizNo(o.getOrderNo());
        rec.setAction("取消派单");
        rec.setFromStatus(Status.WAIT_WORK);
        rec.setToStatus("已取消");
        rec.setOperatorName(UserContext.realName());
        rec.setReason(reason.trim());
        recordMapper.insert(rec);
    }

    private Long operatorIdOfCurrentUser() {
        Operator op = operatorMapper.selectOne(Wrappers.<Operator>lambdaQuery()
                .eq(Operator::getUserId, UserContext.userId()).last("limit 1"));
        return op == null ? -1L : op.getId();
    }

    /* ---- 小工具 ---- */

    private static double d(BigDecimal v) { return v == null ? 0 : v.doubleValue(); }

    static BigDecimal bd(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }

    private static double round(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue(); }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
