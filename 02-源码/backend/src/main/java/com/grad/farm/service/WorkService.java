package com.grad.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * 作业执行与验收。
 *
 * 执行闭环：待作业 --开工--> 作业中 --完工--> 待验收 --验收--> 已完成 / 待返工。
 * 面积偏差：|实际面积 - 报价面积| / 报价面积 > 3% 时，验收单标记需要农户二次确认，
 * 未确认前结算接口拒绝生成结算单。
 */
@Service
@RequiredArgsConstructor
public class WorkService {

    private final DispatchOrderMapper orderMapper;
    private final WorkRecordMapper workRecordMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final ServiceRequestMapper requestMapper;
    private final FarmMachineMapper machineMapper;
    private final OperatorMapper operatorMapper;
    private final FarmRecordMapper recordMapper;
    private final SysMessageMapper messageMapper;
    private final SettlementMapper settlementMapper;
    private final QuotationMapper quotationMapper;
    private final LandMapper landMapper;

    /* ==================== 作业记录 ==================== */

    public PageResult<Map<String, Object>> page(Map<String, String> q) {
        long page = RequestService.num(q.get("page"), 1), size = RequestService.num(q.get("size"), 10);
        QueryWrapper<WorkRecord> w = new QueryWrapper<>();
        if ("SELF".equals(UserContext.dataScope()) && "OPERATOR".equals(UserContext.roleCode())) {
            Operator op = operatorMapper.selectOne(Wrappers.<Operator>lambdaQuery()
                    .eq(Operator::getUserId, UserContext.userId()).last("limit 1"));
            w.eq("operator_id", op == null ? -1L : op.getId());
        }
        if (RequestService.notBlank(q.get("keyword"))) w.like("order_no", q.get("keyword"));
        if (RequestService.notBlank(q.get("status"))) w.eq("record_status", q.get("status"));
        w.orderByDesc("id");
        Page<WorkRecord> p = workRecordMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkRecord r : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", r.getId());
            x.put("orderId", r.getOrderId());
            x.put("orderNo", r.getOrderNo());
            x.put("arriveTime", r.getArriveTime());
            x.put("startTime", r.getStartTime());
            x.put("endTime", r.getEndTime());
            x.put("actualArea", r.getActualArea());
            x.put("fuelLiters", r.getFuelLiters());
            x.put("workHours", r.getWorkHours());
            x.put("beforeImage", r.getBeforeImage());
            x.put("afterImage", r.getAfterImage());
            x.put("exceptionDesc", r.getExceptionDesc());
            x.put("status", r.getRecordStatus());
            DispatchOrder o = orderMapper.selectById(r.getOrderId());
            if (o != null) {
                ServiceRequest req = requestMapper.selectById(o.getRequestId());
                x.put("machineNo", o.getMachineNo());
                x.put("operatorName", o.getOperatorName());
                x.put("planArea", o.getPlanArea());
                if (req != null) {
                    x.put("landName", req.getLandName());
                    x.put("farmerName", req.getFarmerName());
                    x.put("serviceType", req.getServiceType());
                    x.put("crop", req.getCrop());
                }
            }
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 开工：机具置为作业中，需求进入作业中 */
    @Transactional
    public Map<String, Object> start(Requests.WorkStart req) {
        DispatchOrder o = orderMapper.selectById(req.getOrderId());
        if (o == null) throw new BizException(404, "派单不存在");
        if (!Status.WAIT_WORK.equals(o.getDispatchStatus())) {
            throw new BizException("只有「待作业」的派单可以开工，当前为「" + o.getDispatchStatus() + "」");
        }
        Long existing = workRecordMapper.selectCount(Wrappers.<WorkRecord>lambdaQuery()
                .eq(WorkRecord::getOrderId, o.getId()).eq(WorkRecord::getRecordStatus, "进行中"));
        if (existing != null && existing > 0) throw new BizException("该派单已开工，请勿重复提交");

        WorkRecord w = new WorkRecord();
        w.setOrderId(o.getId());
        w.setOrderNo(o.getOrderNo());
        w.setOperatorId(o.getOperatorId());
        w.setArriveTime(req.getArriveTime() == null ? LocalDateTime.now() : req.getArriveTime());
        w.setStartTime(LocalDateTime.now());
        w.setBeforeImage(req.getBeforeImage());
        w.setRecordStatus("进行中");
        w.setRemark(req.getRemark());
        workRecordMapper.insert(w);

        o.setDispatchStatus(Status.WORKING);
        orderMapper.updateById(o);
        FarmMachine m = machineMapper.selectById(o.getMachineId());
        if (m != null) {
            m.setCurrentStatus(Status.M_WORKING);
            machineMapper.updateById(m);
        }
        ServiceRequest r = requestMapper.selectById(o.getRequestId());
        if (r != null) {
            String from = r.getRequestStatus();
            r.setRequestStatus(Status.WORKING);
            requestMapper.updateById(r);
            addRecord("REQUEST", r.getId(), r.getRequestNo(), "机具进场开工", from, Status.WORKING, null);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("workRecord", w);
        res.put("order", o);
        return res;
    }

    /** 完工：写入实际面积/油耗/工时，进入待验收 */
    @Transactional
    public Map<String, Object> finish(Requests.WorkFinish req) {
        DispatchOrder o = orderMapper.selectById(req.getOrderId());
        if (o == null) throw new BizException(404, "派单不存在");
        if (!Status.WORKING.equals(o.getDispatchStatus())) {
            throw new BizException("只有「作业中」的派单可以提交完工，当前为「" + o.getDispatchStatus() + "」");
        }
        if (req.getActualArea() == null || req.getActualArea().doubleValue() <= 0) {
            throw new BizException("实际作业面积必须大于 0");
        }
        WorkRecord w = workRecordMapper.selectOne(Wrappers.<WorkRecord>lambdaQuery()
                .eq(WorkRecord::getOrderId, o.getId()).eq(WorkRecord::getRecordStatus, "进行中")
                .orderByDesc(WorkRecord::getId).last("limit 1"));
        if (w == null) throw new BizException("未找到进行中的作业记录，请先开工");
        w.setEndTime(req.getEndTime() == null ? LocalDateTime.now() : req.getEndTime());
        w.setActualArea(req.getActualArea());
        w.setFuelLiters(req.getFuelLiters());
        w.setWorkHours(req.getWorkHours());
        w.setAfterImage(req.getAfterImage());
        w.setExceptionDesc(req.getExceptionDesc());
        w.setRecordStatus("已完工");
        workRecordMapper.updateById(w);

        o.setDispatchStatus(Status.WAIT_ACCEPT);
        o.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(o);
        FarmMachine m = machineMapper.selectById(o.getMachineId());
        if (m != null) {
            m.setCurrentStatus(Status.M_IDLE);
            m.setTotalHours(m.getTotalHours() == null ? BigDecimal.ZERO
                    : m.getTotalHours().add(req.getWorkHours() == null ? BigDecimal.ZERO : req.getWorkHours()));
            machineMapper.updateById(m);
        }
        Operator op = operatorMapper.selectById(o.getOperatorId());
        if (op != null && req.getWorkHours() != null) {
            op.setTotalHours(op.getTotalHours() == null ? BigDecimal.ZERO : op.getTotalHours().add(req.getWorkHours()));
            operatorMapper.updateById(op);
        }
        ServiceRequest r = requestMapper.selectById(o.getRequestId());
        double planArea = o.getPlanArea() == null ? 0 : o.getPlanArea().doubleValue();
        double diffRate = planArea <= 0 ? 0 : (req.getActualArea().doubleValue() - planArea) / planArea * 100;
        if (r != null) {
            String from = r.getRequestStatus();
            r.setRequestStatus(Status.WAIT_ACCEPT);
            requestMapper.updateById(r);
            addRecord("REQUEST", r.getId(), r.getRequestNo(), "提交完工", from, Status.WAIT_ACCEPT,
                    "实际面积 " + req.getActualArea() + " 亩，面积偏差 " + String.format("%.1f", diffRate) + "%");
            notify(r.getFarmerId(), "作业待验收 " + o.getOrderNo(),
                    "机具已完成 " + r.getServiceType() + " 作业，实际面积 " + req.getActualArea()
                            + " 亩，请确认验收", o.getOrderNo(), "ORDER", "/acceptance");
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("order", o);
        res.put("workRecord", w);
        res.put("areaDiffRate", BigDecimal.valueOf(diffRate).setScale(2, RoundingMode.HALF_UP));
        res.put("needSecondConfirm", Math.abs(diffRate) > Status.AREA_DIFF_THRESHOLD);
        return res;
    }

    /* ==================== 验收 ==================== */

    public PageResult<Map<String, Object>> acceptPage(Map<String, String> q) {
        long page = RequestService.num(q.get("page"), 1), size = RequestService.num(q.get("size"), 10);
        QueryWrapper<DispatchOrder> w = new QueryWrapper<>();
        if ("SELF".equals(UserContext.dataScope()) && "FARMER".equals(UserContext.roleCode())) {
            List<Long> reqIds = requestMapper.selectList(Wrappers.<ServiceRequest>lambdaQuery()
                            .eq(ServiceRequest::getFarmerId, UserContext.userId()))
                    .stream().map(ServiceRequest::getId).toList();
            if (reqIds.isEmpty()) return PageResult.of(0, page, size, List.of());
            w.in("request_id", reqIds);
        }
        if (RequestService.notBlank(q.get("status"))) w.eq("dispatch_status", q.get("status"));
        else w.in("dispatch_status", List.of(Status.WAIT_ACCEPT, Status.FINISHED, Status.REWORK));
        if (RequestService.notBlank(q.get("keyword"))) w.like("order_no", q.get("keyword"));
        w.orderByAsc("dispatch_status").orderByDesc("id");
        Page<DispatchOrder> p = orderMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DispatchOrder o : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", o.getId());
            x.put("orderId", o.getId());
            x.put("orderNo", o.getOrderNo());
            x.put("machineNo", o.getMachineNo());
            x.put("operatorName", o.getOperatorName());
            x.put("planArea", o.getPlanArea());
            x.put("planStart", o.getPlanStart());
            x.put("finishTime", o.getFinishTime());
            x.put("status", o.getDispatchStatus());
            ServiceRequest r = requestMapper.selectById(o.getRequestId());
            if (r != null) {
                x.put("requestNo", r.getRequestNo());
                x.put("farmerName", r.getFarmerName());
                x.put("landName", r.getLandName());
                x.put("serviceType", r.getServiceType());
                x.put("crop", r.getCrop());
                x.put("urgency", r.getUrgency());
            }
            WorkRecord wr = workRecordMapper.selectOne(Wrappers.<WorkRecord>lambdaQuery()
                    .eq(WorkRecord::getOrderId, o.getId()).orderByDesc(WorkRecord::getId).last("limit 1"));
            if (wr != null) {
                x.put("actualArea", wr.getActualArea());
                x.put("fuelLiters", wr.getFuelLiters());
                x.put("workHours", wr.getWorkHours());
                x.put("beforeImage", wr.getBeforeImage());
                x.put("afterImage", wr.getAfterImage());
                double plan = o.getPlanArea() == null ? 0 : o.getPlanArea().doubleValue();
                double diff = plan <= 0 || wr.getActualArea() == null ? 0
                        : (wr.getActualArea().doubleValue() - plan) / plan * 100;
                x.put("areaDiffRate", BigDecimal.valueOf(diff).setScale(2, RoundingMode.HALF_UP));
                x.put("needSecondConfirm", Math.abs(diff) > Status.AREA_DIFF_THRESHOLD);
            }
            Acceptance ac = acceptanceMapper.selectOne(Wrappers.<Acceptance>lambdaQuery()
                    .eq(Acceptance::getOrderId, o.getId()).orderByDesc(Acceptance::getId).last("limit 1"));
            x.put("acceptance", ac);
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /**
     * 农户验收：确认面积与质量；要求返工时订单回到「待返工」，需求回到「待排程」。
     * 验收通过后自动生成结算单（应收 = 验收面积 × 单价 + 路程附加费 + 加急附加费 + 调整）。
     */
    @Transactional
    public Map<String, Object> accept(Requests.AcceptReq req) {
        DispatchOrder o = orderMapper.selectById(req.getOrderId());
        if (o == null) throw new BizException(404, "派单不存在");
        if (!Status.WAIT_ACCEPT.equals(o.getDispatchStatus())) {
            throw new BizException("只有「待验收」的派单可以验收，当前为「" + o.getDispatchStatus() + "」");
        }
        if (req.getAcceptArea() == null || req.getAcceptArea().doubleValue() <= 0) {
            throw new BizException("确认面积必须大于 0");
        }
        ServiceRequest r = requestMapper.selectById(o.getRequestId());
        double plan = o.getPlanArea() == null ? 0 : o.getPlanArea().doubleValue();
        double acceptArea = req.getAcceptArea().doubleValue();
        double diffRate = plan <= 0 ? 0 : (acceptArea - plan) / plan * 100;
        boolean needConfirm = Math.abs(diffRate) > Status.AREA_DIFF_THRESHOLD;
        if (needConfirm && (req.getSecondConfirm() == null || req.getSecondConfirm() != 1)) {
            throw new BizException("面积偏差 " + String.format("%.1f", diffRate)
                    + "% 已超过 3% 阈值，需勾选「已知悉面积偏差并二次确认」后再提交");
        }

        Acceptance ac = new Acceptance();
        ac.setOrderId(o.getId());
        ac.setOrderNo(o.getOrderNo());
        ac.setFarmerId(r == null ? null : r.getFarmerId());
        ac.setAcceptArea(req.getAcceptArea());
        ac.setQualityScore(req.getQualityScore());
        ac.setReworkFlag(req.getReworkFlag() == null ? 0 : req.getReworkFlag());
        ac.setReworkReason(req.getReworkReason());
        ac.setAreaDiffRate(BigDecimal.valueOf(diffRate).setScale(2, RoundingMode.HALF_UP));
        ac.setSecondConfirm(needConfirm ? 1 : 0);
        ac.setOverdueFlag(0);
        ac.setComment(req.getComment());
        ac.setAcceptTime(LocalDateTime.now());
        acceptanceMapper.insert(ac);

        Map<String, Object> res = new LinkedHashMap<>();
        if (ac.getReworkFlag() == 1) {
            if (req.getReworkReason() == null || req.getReworkReason().trim().length() < 5) {
                throw new BizException("要求返工时必须填写返工原因（不少于 5 个字）");
            }
            o.setDispatchStatus(Status.REWORK);
            orderMapper.updateById(o);
            if (r != null) {
                String from = r.getRequestStatus();
                r.setRequestStatus(Status.SCHEDULING);
                r.setBackReason("验收返工：" + req.getReworkReason().trim());
                requestMapper.updateById(r);
                addRecord("REQUEST", r.getId(), r.getRequestNo(), "验收要求返工", from, Status.SCHEDULING,
                        req.getReworkReason().trim());
                notify(2L, "返工待重新排程 " + r.getRequestNo(), req.getReworkReason().trim(),
                        r.getRequestNo(), "REQUEST", "/dispatch");
            }
            res.put("acceptance", ac);
            res.put("result", "已标记返工，需求回到待排程");
            return res;
        }

        o.setDispatchStatus(Status.FINISHED);
        orderMapper.updateById(o);
        if (r != null) {
            String from = r.getRequestStatus();
            r.setRequestStatus(Status.FINISHED);
            requestMapper.updateById(r);
            addRecord("REQUEST", r.getId(), r.getRequestNo(), "农户验收通过", from, Status.FINISHED,
                    req.getComment());
        }
        Settlement st = createSettlement(o, r, acceptArea);
        res.put("acceptance", ac);
        res.put("settlement", st);
        res.put("result", "验收通过，已生成结算单 " + st.getSettleNo());
        return res;
    }

    /** 生成结算单：单价取已确认报价，路程附加费按地块里程重算 */
    @Transactional
    public Settlement createSettlement(DispatchOrder o, ServiceRequest r, double acceptArea) {
        Settlement exists = settlementMapper.selectOne(Wrappers.<Settlement>lambdaQuery()
                .eq(Settlement::getOrderId, o.getId()).last("limit 1"));
        if (exists != null) return exists;
        Quotation qt = o.getQuotationId() == null ? null : quotationMapper.selectById(o.getQuotationId());
        double unit = qt == null || qt.getUnitPrice() == null ? 52 : qt.getUnitPrice().doubleValue();
        double base = unit * acceptArea;
        Land land = r == null ? null : landMapper.selectById(r.getLandId());
        double km = land == null || land.getDistanceKm() == null ? 0 : land.getDistanceKm().doubleValue();
        double road = Math.max(0, km - 10) * 3.5;
        double urgent = qt == null || qt.getUrgentFee() == null ? 0 : qt.getUrgentFee().doubleValue();
        double receivable = base + road + urgent;

        Settlement st = new Settlement();
        st.setSettleNo("JS" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (settlementMapper.selectCount(null) + 1)));
        st.setOrderId(o.getId());
        st.setOrderNo(o.getOrderNo());
        st.setFarmerId(r == null ? null : r.getFarmerId());
        st.setReceivable(BigDecimal.valueOf(receivable).setScale(2, RoundingMode.HALF_UP));
        st.setReceived(BigDecimal.ZERO);
        st.setAdjustAmount(BigDecimal.ZERO);
        st.setSettleStatus(Status.S_UNPAID);
        st.setRemark("按验收面积 " + acceptArea + " 亩计价");
        settlementMapper.insert(st);
        if (r != null) {
            notify(5L, "待登记收款 " + st.getSettleNo(),
                    r.getFarmerName() + " 应收 " + st.getReceivable() + " 元", st.getSettleNo(), "SETTLE", "/settlement");
            notify(r.getFarmerId(), "结算单已生成 " + st.getSettleNo(),
                    "本次作业应收 " + st.getReceivable() + " 元，请按约定方式线下支付",
                    st.getSettleNo(), "SETTLE", "/acceptance");
        }
        return st;
    }

    /* ---- 内部工具 ---- */

    private void addRecord(String bizType, Long bizId, String bizNo, String action,
                           String from, String to, String reason) {
        FarmRecord rec = new FarmRecord();
        rec.setBizType(bizType);
        rec.setBizId(bizId);
        rec.setBizNo(bizNo);
        rec.setAction(action);
        rec.setFromStatus(from);
        rec.setToStatus(to);
        rec.setOperatorName(UserContext.realName());
        rec.setReason(reason);
        recordMapper.insert(rec);
    }

    private void notify(Long receiverId, String title, String content, String bizNo, String bizType, String url) {
        if (receiverId == null) return;
        SysMessage m = new SysMessage();
        m.setReceiverId(receiverId);
        m.setTitle(title);
        m.setContent(content);
        m.setBizNo(bizNo);
        m.setBizType(bizType);
        m.setUrl(url);
        m.setIsRead(0);
        messageMapper.insert(m);
    }
}
