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
 * 结算管理：应收按验收面积计算，登记线下收款（现金/微信/银行转账/挂账）。
 * 规则：已收款（settle_status=已收款）的单据不可再修改金额，减免只允许追加调整单。
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementMapper settlementMapper;
    private final DispatchOrderMapper orderMapper;
    private final ServiceRequestMapper requestMapper;
    private final WorkRecordMapper workRecordMapper;
    private final FarmRecordMapper recordMapper;
    private final SysMessageMapper messageMapper;

    public PageResult<Map<String, Object>> page(Map<String, String> q) {
        long page = RequestService.num(q.get("page"), 1), size = RequestService.num(q.get("size"), 10);
        QueryWrapper<Settlement> w = new QueryWrapper<>();
        if (RequestService.notBlank(q.get("status"))) w.eq("settle_status", q.get("status"));
        if (RequestService.notBlank(q.get("keyword"))) {
            w.and(x -> x.like("settle_no", q.get("keyword")).or().like("order_no", q.get("keyword")));
        }
        if (RequestService.notBlank(q.get("startDate"))) w.ge("create_time", q.get("startDate") + " 00:00:00");
        if (RequestService.notBlank(q.get("endDate"))) w.le("create_time", q.get("endDate") + " 23:59:59");
        w.orderByDesc("id");
        Page<Settlement> p = settlementMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Settlement s : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", s.getId());
            x.put("settleNo", s.getSettleNo());
            x.put("orderNo", s.getOrderNo());
            x.put("receivable", s.getReceivable());
            x.put("received", s.getReceived());
            x.put("adjustAmount", s.getAdjustAmount());
            x.put("status", s.getSettleStatus());
            x.put("payMethod", s.getPayMethod());
            x.put("voucherImage", s.getVoucherImage());
            x.put("settleTime", s.getSettleTime());
            x.put("remark", s.getRemark());
            x.put("createTime", s.getCreateTime());
            x.put("unreceived", s.getReceivable() == null ? BigDecimal.ZERO
                    : s.getReceivable().subtract(s.getReceived() == null ? BigDecimal.ZERO : s.getReceived())
                    .add(s.getAdjustAmount() == null ? BigDecimal.ZERO : s.getAdjustAmount()));
            DispatchOrder o = orderMapper.selectById(s.getOrderId());
            if (o != null) {
                x.put("machineNo", o.getMachineNo());
                x.put("operatorName", o.getOperatorName());
                ServiceRequest r = requestMapper.selectById(o.getRequestId());
                if (r != null) {
                    x.put("farmerName", r.getFarmerName());
                    x.put("landName", r.getLandName());
                    x.put("serviceType", r.getServiceType());
                    x.put("crop", r.getCrop());
                }
                WorkRecord wr = workRecordMapper.selectOne(Wrappers.<WorkRecord>lambdaQuery()
                        .eq(WorkRecord::getOrderId, o.getId()).orderByDesc(WorkRecord::getId).last("limit 1"));
                x.put("actualArea", wr == null ? null : wr.getActualArea());
            }
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> detail(Long id) {
        Settlement s = settlementMapper.selectById(id);
        if (s == null) throw new BizException(404, "结算单不存在");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("settlement", s);
        DispatchOrder o = orderMapper.selectById(s.getOrderId());
        res.put("order", o);
        res.put("request", o == null ? null : requestMapper.selectById(o.getRequestId()));
        res.put("workRecord", o == null ? null : workRecordMapper.selectOne(Wrappers.<WorkRecord>lambdaQuery()
                .eq(WorkRecord::getOrderId, o.getId()).orderByDesc(WorkRecord::getId).last("limit 1")));
        return res;
    }

    /** 登记线下收款 */
    @Transactional
    public Settlement pay(Requests.PayReq req) {
        Settlement s = settlementMapper.selectById(req.getId());
        if (s == null) throw new BizException(404, "结算单不存在");
        if (Status.S_PAID.equals(s.getSettleStatus())) {
            throw new BizException("该结算单已收款完成，不可重复登记；如需减免请走线下调整单");
        }
        if (req.getReceived() == null || req.getReceived().doubleValue() < 0) {
            throw new BizException("收款金额不合法");
        }
        BigDecimal adjust = s.getAdjustAmount() == null ? BigDecimal.ZERO : s.getAdjustAmount();
        BigDecimal target = s.getReceivable().add(adjust);
        BigDecimal totalReceived = (s.getReceived() == null ? BigDecimal.ZERO : s.getReceived()).add(req.getReceived());
        if (totalReceived.compareTo(target) > 0.009) {
            throw new BizException("累计收款 " + totalReceived + " 元超过应收 " + target + " 元，请核对");
        }
        s.setReceived(totalReceived.setScale(2, RoundingMode.HALF_UP));
        s.setPayMethod(req.getPayMethod());
        if (req.getVoucherImage() != null && !req.getVoucherImage().isBlank()) {
            s.setVoucherImage(req.getVoucherImage());
        }
        s.setSettleStatus(totalReceived.compareTo(target) >= 0 ? Status.S_PAID : Status.S_PART);
        s.setSettleTime(LocalDateTime.now());
        s.setFinanceId(UserContext.userId());
        if (req.getRemark() != null && !req.getRemark().isBlank()) s.setRemark(req.getRemark());
        settlementMapper.updateById(s);

        FarmRecord rec = new FarmRecord();
        rec.setBizType("SETTLE");
        rec.setBizId(s.getId());
        rec.setBizNo(s.getSettleNo());
        rec.setAction("登记收款");
        rec.setFromStatus(Status.S_UNPAID);
        rec.setToStatus(s.getSettleStatus());
        rec.setOperatorName(UserContext.realName());
        rec.setReason(req.getPayMethod() + " 收款 " + req.getReceived() + " 元");
        recordMapper.insert(rec);
        return s;
    }

    /** 人工减免（负数表示减免），只允许在未收款完成前登记 */
    @Transactional
    public Settlement adjust(Long id, BigDecimal amount, String reason) {
        Settlement s = settlementMapper.selectById(id);
        if (s == null) throw new BizException(404, "结算单不存在");
        if (Status.S_PAID.equals(s.getSettleStatus())) {
            throw new BizException("已收款完成的单据不可修改金额，请走线下调整单");
        }
        if (reason == null || reason.trim().length() < 5) throw new BizException("调整原因不少于 5 个字");
        s.setAdjustAmount(amount == null ? BigDecimal.ZERO : amount);
        s.setRemark("人工调整：" + reason.trim());
        settlementMapper.updateById(s);
        return s;
    }
}
