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
 * 创新点「机具能力与成本联合派单」的规则配置与计算留痕。
 * 规则按版本管理：保存即生成新版本并停用旧版本，历史评分记录的 rule_version 不被覆盖。
 */
@Service
@RequiredArgsConstructor
public class RuleService {

    private final FarmRuleMapper ruleMapper;
    private final FarmScoreMapper scoreMapper;
    private final ServiceRequestMapper requestMapper;
    private final FarmMachineMapper machineMapper;
    private final OperatorMapper operatorMapper;

    public List<FarmRule> list() {
        return ruleMapper.selectList(Wrappers.<FarmRule>lambdaQuery().orderByDesc(FarmRule::getId));
    }

    /** 保存规则：权重合计必须 100%，保存后新版本生效、旧版本停用 */
    @Transactional
    public FarmRule save(Requests.RuleSave req) {
        Map<String, BigDecimal> w = req.getWeights();
        if (w == null || w.isEmpty()) throw new BizException("权重不能为空");
        BigDecimal sum = w.values().stream().filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        }
        for (String k : List.of("capability", "cert", "free", "distance", "fuel", "ontime")) {
            if (w.get(k) == null || w.get(k).doubleValue() < 0) {
                throw new BizException("权重项 " + k + " 缺失或为负数");
            }
        }
        BigDecimal high = req.getLevelHigh() == null ? new BigDecimal("80") : req.getLevelHigh();
        BigDecimal mid = req.getLevelMid() == null ? new BigDecimal("60") : req.getLevelMid();
        if (mid.compareTo(high) >= 0) throw new BizException("「可选」阈值必须小于「推荐」阈值");

        // 旧版本停用
        for (FarmRule old : ruleMapper.selectList(Wrappers.<FarmRule>lambdaQuery()
                .eq(FarmRule::getRuleStatus, "启用"))) {
            old.setRuleStatus("停用");
            ruleMapper.updateById(old);
        }
        long count = ruleMapper.selectCount(null);
        FarmRule r = new FarmRule();
        r.setRuleName(req.getRuleName() == null ? "机具能力与成本联合派单" : req.getRuleName());
        r.setRuleVersion(req.getRuleVersion() == null ? "V" + (count + 1) + ".0" : req.getRuleVersion());
        r.setWCapability(w.get("capability"));
        r.setWCert(w.get("cert"));
        r.setWFree(w.get("free"));
        r.setWDistance(w.get("distance"));
        r.setWFuel(w.get("fuel"));
        r.setWOntime(w.get("ontime"));
        r.setLevelHigh(high);
        r.setLevelMid(mid);
        r.setRuleStatus("启用");
        r.setEffectiveTime(LocalDateTime.now());
        r.setRemark(req.getRemark());
        r.setCreateBy(UserContext.realName());
        ruleMapper.insert(r);
        return r;
    }

    /* ==================== 计算留痕 ==================== */

    public PageResult<Map<String, Object>> scorePage(Map<String, String> q) {
        long page = RequestService.num(q.get("page"), 1), size = RequestService.num(q.get("size"), 10);
        QueryWrapper<FarmScore> w = new QueryWrapper<>();
        if (RequestService.notBlank(q.get("keyword"))) {
            w.and(x -> x.like("request_no", q.get("keyword")).or().like("machine_no", q.get("keyword"))
                    .or().like("operator_name", q.get("keyword")));
        }
        if (RequestService.notBlank(q.get("level"))) w.eq("result_level", q.get("level"));
        if (RequestService.notBlank(q.get("requestId"))) w.eq("request_id", q.get("requestId"));
        w.orderByDesc("id");
        Page<FarmScore> p = scoreMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FarmScore s : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", s.getId());
            x.put("ruleVersion", s.getRuleVersion());
            x.put("requestId", s.getRequestId());
            x.put("requestNo", s.getRequestNo());
            x.put("machineId", s.getMachineId());
            x.put("machineNo", s.getMachineNo());
            x.put("operatorName", s.getOperatorName());
            x.put("capabilityScore", s.getCapabilityScore());
            x.put("certScore", s.getCertScore());
            x.put("freeScore", s.getFreeScore());
            x.put("distanceScore", s.getDistanceScore());
            x.put("fuelScore", s.getFuelScore());
            x.put("ontimeScore", s.getOntimeScore());
            x.put("totalScore", s.getTotalScore());
            x.put("resultLevel", s.getResultLevel());
            x.put("finalLevel", s.getFinalLevel());
            x.put("explainText", s.getExplainText());
            x.put("unmetText", s.getUnmetText());
            x.put("improveText", s.getImproveText());
            x.put("snapshot", s.getSnapshot());
            x.put("adjustReason", s.getAdjustReason());
            x.put("adjustBy", s.getAdjustBy());
            x.put("adjustTime", s.getAdjustTime());
            x.put("createTime", s.getCreateTime());
            x.put("adjusted", s.getAdjustReason() != null && !s.getAdjustReason().isBlank());
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> scoreDetail(Long id) {
        FarmScore s = scoreMapper.selectById(id);
        if (s == null) throw new BizException(404, "计算记录不存在");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("score", s);
        res.put("request", requestMapper.selectById(s.getRequestId()));
        res.put("machine", machineMapper.selectById(s.getMachineId()));
        res.put("operator", s.getOperatorId() == null ? null : operatorMapper.selectById(s.getOperatorId()));
        return res;
    }

    /** 人工调整最终等级：原始 result_level 保留，调整写 final_level 与原因 */
    @Transactional
    public FarmScore adjust(Long id, Requests.ScoreAdjust req) {
        FarmScore s = scoreMapper.selectById(id);
        if (s == null) throw new BizException(404, "计算记录不存在");
        if (!List.of(Status.LV_HIGH, Status.LV_MID, Status.LV_LOW).contains(req.getFinalLevel())) {
            throw new BizException("目标等级不合法");
        }
        if (req.getReason() == null || req.getReason().trim().length() < 5) {
            throw new BizException("人工调整必须填写原因（不少于 5 个字）");
        }
        s.setFinalLevel(req.getFinalLevel());
        s.setAdjustReason(req.getReason().trim());
        s.setAdjustBy(UserContext.realName());
        s.setAdjustTime(LocalDateTime.now());
        scoreMapper.updateById(s);
        return s;
    }

    /** 创新模块效果统计：推荐等级分布、人工调整率、采纳率 */
    public Map<String, Object> effect() {
        List<FarmScore> all = scoreMapper.selectList(null);
        long total = all.size();
        long high = all.stream().filter(s -> Status.LV_HIGH.equals(s.getResultLevel())).count();
        long mid = all.stream().filter(s -> Status.LV_MID.equals(s.getResultLevel())).count();
        long low = all.stream().filter(s -> Status.LV_LOW.equals(s.getResultLevel())).count();
        long adjusted = all.stream().filter(s -> s.getAdjustReason() != null && !s.getAdjustReason().isBlank()).count();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", total);
        res.put("high", high);
        res.put("mid", mid);
        res.put("low", low);
        res.put("adjusted", adjusted);
        res.put("adjustRate", total == 0 ? 0
                : BigDecimal.valueOf(adjusted * 100.0 / total).setScale(1, RoundingMode.HALF_UP));
        res.put("adoptRate", total == 0 ? 0
                : BigDecimal.valueOf(high * 100.0 / total).setScale(1, RoundingMode.HALF_UP));
        return res;
    }
}
