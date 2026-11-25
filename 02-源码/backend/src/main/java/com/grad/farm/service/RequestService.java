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
 * 作业需求与报价。
 *
 * 报价金额全部由后端计算：基础作业费 = 单价 × 面积；路程附加费 = max(0, 距离 - 免费里程) × 里程单价；
 * 加急附加费 = 基础作业费 × (加急系数 - 1)；其他附加费由调度员手工填写。
 */
@Service
@RequiredArgsConstructor
public class RequestService {

    private final ServiceRequestMapper requestMapper;
    private final QuotationMapper quotationMapper;
    private final ServicePriceMapper priceMapper;
    private final LandMapper landMapper;
    private final FarmMachineMapper machineMapper;
    private final DispatchOrderMapper orderMapper;
    private final FarmRecordMapper recordMapper;
    private final SysMessageMapper messageMapper;
    private final OperatorMapper operatorMapper;

    public PageResult<Map<String, Object>> page(Map<String, String> q) {
        long page = num(q.get("page"), 1), size = num(q.get("size"), 10);
        QueryWrapper<ServiceRequest> w = new QueryWrapper<>();
        if ("SELF".equals(UserContext.dataScope())) {
            // 农户只能看本人提交的需求
            w.eq("farmer_id", UserContext.userId());
        }
        if (notBlank(q.get("keyword"))) {
            w.and(x -> x.like("request_no", q.get("keyword")).or()
                    .like("farmer_name", q.get("keyword")).or().like("land_name", q.get("keyword")));
        }
        if (notBlank(q.get("status"))) w.eq("request_status", q.get("status"));
        if (notBlank(q.get("serviceType"))) w.eq("service_type", q.get("serviceType"));
        if (notBlank(q.get("crop"))) w.eq("crop", q.get("crop"));
        if (notBlank(q.get("urgency"))) w.eq("urgency", q.get("urgency"));
        if (notBlank(q.get("startDate"))) w.ge("create_time", q.get("startDate") + " 00:00:00");
        if (notBlank(q.get("endDate"))) w.le("create_time", q.get("endDate") + " 23:59:59");
        w.orderByDesc("create_time");
        Page<ServiceRequest> p = requestMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ServiceRequest r : p.getRecords()) rows.add(vo(r));
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    private Map<String, Object> vo(ServiceRequest r) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", r.getId());
        x.put("requestNo", r.getRequestNo());
        x.put("farmerId", r.getFarmerId());
        x.put("farmerName", r.getFarmerName());
        x.put("landId", r.getLandId());
        x.put("landName", r.getLandName());
        x.put("serviceType", r.getServiceType());
        x.put("crop", r.getCrop());
        x.put("area", r.getArea());
        x.put("hopeDate", r.getHopeDate());
        x.put("urgency", r.getUrgency());
        x.put("requireDesc", r.getRequireDesc());
        x.put("status", r.getRequestStatus());
        x.put("backReason", r.getBackReason());
        x.put("totalAmount", r.getTotalAmount());
        x.put("createTime", r.getCreateTime());
        x.put("updateTime", r.getUpdateTime());
        return x;
    }

    /** 需求详情：基础信息 + 报价 + 派单 + 时间轴（详情页不复用列表接口） */
    public Map<String, Object> detail(Long id) {
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException(404, "需求不存在");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("request", vo(r));
        res.put("quotations", quotationMapper.selectList(Wrappers.<Quotation>lambdaQuery()
                .eq(Quotation::getRequestId, id).orderByDesc(Quotation::getId)));
        List<DispatchOrder> orders = orderMapper.selectList(Wrappers.<DispatchOrder>lambdaQuery()
                .eq(DispatchOrder::getRequestId, id).orderByDesc(DispatchOrder::getId));
        res.put("orders", orders);
        res.put("records", recordMapper.selectList(Wrappers.<FarmRecord>lambdaQuery()
                .eq(FarmRecord::getBizType, "REQUEST").eq(FarmRecord::getBizId, id)
                .orderByAsc(FarmRecord::getId)));
        res.put("land", r.getLandId() == null ? null : landMapper.selectById(r.getLandId()));
        return res;
    }

    /** 新建/修改需求：只有「待报价」「已退回」允许编辑 */
    @Transactional
    public Long save(Requests.RequestSave req) {
        if (req.getLandId() == null) throw new BizException("请选择地块");
        if (!notBlank(req.getServiceType())) throw new BizException("请选择服务类型");
        if (req.getArea() == null || req.getArea().doubleValue() <= 0) throw new BizException("面积必须大于 0");
        if (req.getHopeDate() == null) throw new BizException("请选择希望作业日期");
        Land land = landMapper.selectById(req.getLandId());
        if (land == null) throw new BizException(404, "地块不存在");

        ServiceRequest r;
        if (req.getId() == null) {
            r = new ServiceRequest();
            r.setRequestNo("XQ" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%04d", (requestMapper.selectCount(null) + 1)));
            r.setFarmerId(UserContext.userId());
            r.setFarmerName(UserContext.realName());
            r.setRequestStatus(Status.QUOTING);
        } else {
            r = requestMapper.selectById(req.getId());
            if (r == null) throw new BizException(404, "需求不存在");
            if (!Status.QUOTING.equals(r.getRequestStatus()) && !Status.RETURNED.equals(r.getRequestStatus())) {
                throw new BizException("当前状态「" + r.getRequestStatus() + "」不允许修改需求");
            }
            r.setRequestStatus(Status.QUOTING);
            r.setBackReason(null);
        }
        r.setLandId(land.getId());
        r.setLandName(land.getLandName());
        r.setServiceType(req.getServiceType());
        r.setCrop(notBlank(req.getCrop()) ? req.getCrop() : land.getCrop());
        r.setArea(req.getArea());
        r.setHopeDate(req.getHopeDate());
        r.setUrgency(notBlank(req.getUrgency()) ? req.getUrgency() : "普通");
        r.setRequireDesc(req.getRequireDesc());
        if (req.getId() == null) requestMapper.insert(r); else requestMapper.updateById(r);

        addRecord("REQUEST", r.getId(), r.getRequestNo(), req.getId() == null ? "提交需求" : "修改需求",
                null, Status.QUOTING, null);
        if (req.getId() == null) {
            notify(2L, "新需求待报价 " + r.getRequestNo(),
                    r.getFarmerName() + " 提交了 " + r.getCrop() + " " + r.getServiceType() + " "
                            + r.getArea() + " 亩的需求", r.getRequestNo(), "REQUEST", "/request");
        }
        return r.getId();
    }

    /** 生成报价（调度员） */
    @Transactional
    public Map<String, Object> generateQuote(Requests.QuoteGen req) {
        ServiceRequest r = requestMapper.selectById(req.getRequestId());
        if (r == null) throw new BizException(404, "需求不存在");
        if (!Status.QUOTING.equals(r.getRequestStatus())) {
            throw new BizException("只有「待报价」的需求可以生成报价，当前为「" + r.getRequestStatus() + "」");
        }
        Land land = landMapper.selectById(r.getLandId());
        ServicePrice price = priceMapper.selectOne(Wrappers.<ServicePrice>lambdaQuery()
                .eq(ServicePrice::getServiceType, r.getServiceType())
                .eq(ServicePrice::getCrop, r.getCrop())
                .eq(ServicePrice::getPriceStatus, "启用").last("limit 1"));
        if (price == null) {
            price = priceMapper.selectOne(Wrappers.<ServicePrice>lambdaQuery()
                    .eq(ServicePrice::getServiceType, r.getServiceType())
                    .eq(ServicePrice::getPriceStatus, "启用").last("limit 1"));
        }
        if (price == null) throw new BizException("未配置「" + r.getServiceType() + "」的服务价目，请先在服务价目中维护");

        double area = r.getArea().doubleValue();
        double unit = price.getBasePrice().doubleValue();
        double baseAmount = unit * area;
        double km = land == null || land.getDistanceKm() == null ? 0 : land.getDistanceKm().doubleValue();
        double freeKm = price.getFreeKm() == null ? 0 : price.getFreeKm().doubleValue();
        double perKm = price.getRoadFeePerKm() == null ? 0 : price.getRoadFeePerKm().doubleValue();
        double roadFee = Math.max(0, km - freeKm) * perKm;
        double urgentFee = 0;
        if (!"普通".equals(r.getUrgency()) && price.getUrgentRate() != null) {
            urgentFee = baseAmount * (price.getUrgentRate().doubleValue() - 1);
        }
        double extraFee = req.getExtraFee() == null ? 0 : req.getExtraFee().doubleValue();
        double total = baseAmount + roadFee + urgentFee + extraFee;

        Quotation qt = new Quotation();
        qt.setQuotationNo("BJ" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", (quotationMapper.selectCount(null) + 1)));
        qt.setRequestId(r.getId());
        qt.setRequestNo(r.getRequestNo());
        qt.setUnitPrice(bd(unit));
        qt.setArea(r.getArea());
        qt.setBaseAmount(bd(baseAmount));
        qt.setRoadFee(bd(roadFee));
        qt.setUrgentFee(bd(urgentFee));
        qt.setExtraFee(bd(extraFee));
        qt.setTotalAmount(bd(total));
        qt.setQuoteStatus("待确认");
        qt.setExpireTime(LocalDateTime.now().plusDays(7));
        qt.setRemark(req.getRemark());
        qt.setCreateBy(UserContext.realName());
        quotationMapper.insert(qt);

        r.setRequestStatus(Status.CONFIRMING);
        r.setTotalAmount(bd(total));
        r.setDispatcherId(UserContext.userId());
        requestMapper.updateById(r);
        addRecord("REQUEST", r.getId(), r.getRequestNo(), "生成报价", Status.QUOTING, Status.CONFIRMING,
                "报价合计 " + bd(total) + " 元");
        notify(r.getFarmerId(), "报价待确认 " + qt.getQuotationNo(),
                r.getRequestNo() + " 已生成报价 " + bd(total) + " 元，请确认后进入排程",
                r.getRequestNo(), "REQUEST", "/request");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("quotation", qt);
        res.put("breakdown", List.of(
                Map.of("name", "基础作业费", "formula", unit + " 元/亩 × " + area + " 亩", "amount", bd(baseAmount)),
                Map.of("name", "路程附加费", "formula", "超出 " + Math.max(0, km - freeKm) + " 公里 × " + perKm + " 元", "amount", bd(roadFee)),
                Map.of("name", "加急附加费", "formula", "基础费 × (" + (price.getUrgentRate() == null ? "1" : price.getUrgentRate()) + " - 1)", "amount", bd(urgentFee)),
                Map.of("name", "其他附加费", "formula", "调度员手工填写", "amount", bd(extraFee))));
        return res;
    }

    /** 农户确认报价 → 进入待排程 */
    @Transactional
    public void confirmQuote(Long requestId) {
        ServiceRequest r = requestMapper.selectById(requestId);
        if (r == null) throw new BizException(404, "需求不存在");
        if (!Status.CONFIRMING.equals(r.getRequestStatus())) {
            throw new BizException("只有「待确认」的需求可以确认报价");
        }
        Quotation qt = quotationMapper.selectOne(Wrappers.<Quotation>lambdaQuery()
                .eq(Quotation::getRequestId, requestId).eq(Quotation::getQuoteStatus, "待确认")
                .orderByDesc(Quotation::getId).last("limit 1"));
        if (qt == null) throw new BizException("没有待确认的报价单");
        qt.setQuoteStatus("已确认");
        qt.setConfirmTime(LocalDateTime.now());
        quotationMapper.updateById(qt);
        r.setRequestStatus(Status.SCHEDULING);
        requestMapper.updateById(r);
        addRecord("REQUEST", r.getId(), r.getRequestNo(), "农户确认报价", Status.CONFIRMING, Status.SCHEDULING,
                "确认金额 " + qt.getTotalAmount() + " 元");
        notify(2L, "报价已确认 " + r.getRequestNo(), "农户 " + r.getFarmerName() + " 已确认报价，请安排排程",
                r.getRequestNo(), "REQUEST", "/dispatch");
    }

    /** 调度员退回需求 */
    @Transactional
    public void back(Long id, String reason) {
        if (reason == null || reason.trim().length() < 5) throw new BizException("退回原因不少于 5 个字");
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException(404, "需求不存在");
        String from = r.getRequestStatus();
        if (!Status.QUOTING.equals(from) && !Status.CONFIRMING.equals(from) && !Status.SCHEDULING.equals(from)) {
            throw new BizException("当前状态「" + from + "」不可退回");
        }
        r.setRequestStatus(Status.RETURNED);
        r.setBackReason(reason.trim());
        requestMapper.updateById(r);
        addRecord("REQUEST", r.getId(), r.getRequestNo(), "退回需求", from, Status.RETURNED, reason.trim());
        notify(r.getFarmerId(), "需求被退回 " + r.getRequestNo(), reason.trim(), r.getRequestNo(), "REQUEST", "/request");
    }

    /** 农户取消需求 */
    @Transactional
    public void cancel(Long id, String reason) {
        ServiceRequest r = requestMapper.selectById(id);
        if (r == null) throw new BizException(404, "需求不存在");
        if (Status.FINISHED.equals(r.getRequestStatus()) || Status.CANCELLED.equals(r.getRequestStatus())) {
            throw new BizException("当前状态不可取消");
        }
        if (!Status.QUOTING.equals(r.getRequestStatus()) && !Status.RETURNED.equals(r.getRequestStatus())) {
            throw new BizException("已进入报价确认的需求需联系调度员处理");
        }
        String from = r.getRequestStatus();
        r.setRequestStatus(Status.CANCELLED);
        requestMapper.updateById(r);
        addRecord("REQUEST", r.getId(), r.getRequestNo(), "取消需求", from, Status.CANCELLED,
                notBlank(reason) ? reason : "农户主动取消");
    }

    public List<ServiceRequest> schedulableRequests() {
        return requestMapper.selectList(Wrappers.<ServiceRequest>lambdaQuery()
                .in(ServiceRequest::getRequestStatus, List.of(Status.SCHEDULING, Status.WAIT_WORK, Status.WORKING))
                .orderByAsc(ServiceRequest::getHopeDate));
    }

    /* ---------------- 内部工具 ---------------- */

    void addRecord(String bizType, Long bizId, String bizNo, String action,
                   String from, String to, String reason) {
        FarmRecord r = new FarmRecord();
        r.setBizType(bizType);
        r.setBizId(bizId);
        r.setBizNo(bizNo);
        r.setAction(action);
        r.setFromStatus(from);
        r.setToStatus(to);
        r.setOperatorName(UserContext.realName());
        r.setReason(reason);
        recordMapper.insert(r);
    }

    void notify(Long receiverId, String title, String content, String bizNo, String bizType, String url) {
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

    static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    static long num(String s, long def) {
        try { return s == null ? def : Long.parseLong(s); } catch (Exception e) { return def; }
    }
}
