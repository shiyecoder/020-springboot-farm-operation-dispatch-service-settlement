package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.Settlement;
import com.grad.farm.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 结算与线下收款登记。 */
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/page")
    public R<Object> page(@RequestParam Map<String, String> q) {
        return R.ok(settlementService.page(q));
    }

    @GetMapping("/{id}")
    public R<Object> detail(@PathVariable Long id) {
        return R.ok(settlementService.detail(id));
    }

    @PostMapping("/pay")
    public R<Settlement> pay(@RequestBody Requests.PayReq req) {
        return R.ok(settlementService.pay(req));
    }

    @PostMapping("/{id}/adjust")
    public R<Settlement> adjust(@PathVariable Long id,
                                @RequestParam BigDecimal amount,
                                @RequestParam String reason) {
        return R.ok(settlementService.adjust(id, amount, reason));
    }
}
