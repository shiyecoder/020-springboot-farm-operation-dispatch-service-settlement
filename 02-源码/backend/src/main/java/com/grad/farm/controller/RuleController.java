package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.FarmRule;
import com.grad.farm.entity.FarmScore;
import com.grad.farm.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 创新点「机具能力与成本联合派单」的规则配置、计算留痕与人工调整。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping("/rule/list")
    public R<List<FarmRule>> list() {
        return R.ok(ruleService.list());
    }

    @PostMapping("/rule")
    public R<FarmRule> save(@RequestBody Requests.RuleSave req) {
        return R.ok(ruleService.save(req));
    }

    @GetMapping("/rule/effect")
    public R<Object> effect() {
        return R.ok(ruleService.effect());
    }

    @GetMapping("/score/page")
    public R<Object> scorePage(@RequestParam Map<String, String> q) {
        return R.ok(ruleService.scorePage(q));
    }

    @GetMapping("/score/{id}")
    public R<Object> scoreDetail(@PathVariable Long id) {
        return R.ok(ruleService.scoreDetail(id));
    }

    @PostMapping("/score/{id}/adjust")
    public R<FarmScore> adjust(@PathVariable Long id, @RequestBody Requests.ScoreAdjust req) {
        return R.ok(ruleService.adjust(id, req));
    }
}
