package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.DispatchOrder;
import com.grad.farm.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 智能排程台：推荐计算、派单、派单列表与详情。 */
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    /** 试算候选组合（不落库） */
    @GetMapping("/candidates")
    public R<Object> candidates(@RequestParam Long requestId) {
        return R.ok(dispatchService.candidates(requestId));
    }

    /** 正式计算并保存留痕 */
    @PostMapping("/evaluate")
    public R<Object> evaluate(@RequestBody Requests.EvaluateReq req) {
        return R.ok(dispatchService.evaluate(req));
    }

    @PostMapping("/dispatch")
    public R<DispatchOrder> dispatch(@RequestBody Requests.DispatchReq req) {
        return R.ok(dispatchService.dispatch(req));
    }

    @GetMapping("/order/page")
    public R<Object> orderPage(@RequestParam Map<String, String> q) {
        return R.ok(dispatchService.orderPage(q));
    }

    @GetMapping("/order/{id}")
    public R<Object> orderDetail(@PathVariable Long id) {
        return R.ok(dispatchService.orderDetail(id));
    }

    @PostMapping("/order/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @RequestBody Requests.BackReq req) {
        dispatchService.cancelOrder(id, req.getReason());
        return R.ok();
    }
}
