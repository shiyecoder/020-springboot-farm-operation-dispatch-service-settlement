package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 作业执行与验收。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    @GetMapping("/work/page")
    public R<Object> page(@RequestParam Map<String, String> q) {
        return R.ok(workService.page(q));
    }

    @PostMapping("/work/start")
    public R<Object> start(@RequestBody Requests.WorkStart req) {
        return R.ok(workService.start(req));
    }

    @PostMapping("/work/finish")
    public R<Object> finish(@RequestBody Requests.WorkFinish req) {
        return R.ok(workService.finish(req));
    }

    @GetMapping("/acceptance/page")
    public R<Object> acceptPage(@RequestParam Map<String, String> q) {
        return R.ok(workService.acceptPage(q));
    }

    @PostMapping("/acceptance/accept")
    public R<Object> accept(@RequestBody Requests.AcceptReq req) {
        return R.ok(workService.accept(req));
    }
}
