package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.ServiceRequest;
import com.grad.farm.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 作业需求与报价确认。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/request/page")
    public R<Object> page(@RequestParam Map<String, String> q) {
        return R.ok(requestService.page(q));
    }

    @GetMapping("/request/{id}")
    public R<Object> detail(@PathVariable Long id) {
        return R.ok(requestService.detail(id));
    }

    @PostMapping("/request")
    public R<Long> save(@RequestBody Requests.RequestSave req) {
        return R.ok(requestService.save(req));
    }

    @PostMapping("/request/{id}/back")
    public R<Void> back(@PathVariable Long id, @RequestBody Requests.BackReq req) {
        requestService.back(id, req.getReason());
        return R.ok();
    }

    @PostMapping("/request/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Requests.BackReq req) {
        requestService.cancel(id, req == null ? null : req.getReason());
        return R.ok();
    }

    @GetMapping("/request/schedulable")
    public R<List<ServiceRequest>> schedulable() {
        return R.ok(requestService.schedulableRequests());
    }

    /* ---------- 报价 ---------- */

    @PostMapping("/quotation/generate")
    public R<Object> generate(@RequestBody Requests.QuoteGen req) {
        return R.ok(requestService.generateQuote(req));
    }

    @PostMapping("/quotation/{requestId}/confirm")
    public R<Void> confirm(@PathVariable Long requestId) {
        requestService.confirmQuote(requestId);
        return R.ok();
    }
}
