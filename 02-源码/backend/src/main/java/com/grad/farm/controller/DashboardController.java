package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 运营看板：指标与图表聚合。 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public R<Object> overview(@RequestParam(required = false, defaultValue = "month") String period) {
        return R.ok(dashboardService.overview(period));
    }
}
