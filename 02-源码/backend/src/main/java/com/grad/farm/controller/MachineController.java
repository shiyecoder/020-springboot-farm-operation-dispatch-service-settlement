package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.*;
import com.grad.farm.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 资源主数据：机具、农机手、服务价目、地块、保养维修。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    /* ---------- 机具 ---------- */

    @GetMapping("/machine/page")
    public R<Object> machinePage(@RequestParam Map<String, String> q) {
        return R.ok(machineService.machinePage(q));
    }

    @GetMapping("/machine/{id}")
    public R<Object> machineDetail(@PathVariable Long id) {
        return R.ok(machineService.machineDetail(id));
    }

    @PostMapping("/machine")
    public R<Void> saveMachine(@RequestBody Requests.MachineSave req) {
        machineService.saveMachine(req);
        return R.ok();
    }

    @PostMapping("/machine/{id}/status")
    public R<Void> machineStatus(@PathVariable Long id, @RequestBody Requests.MachineStatus req) {
        machineService.changeMachineStatus(id, req);
        return R.ok();
    }

    @GetMapping("/machine/options")
    public R<List<FarmMachine>> machineOptions() {
        return R.ok(machineService.machineOptions());
    }

    /* ---------- 农机手 ---------- */

    @GetMapping("/operator/page")
    public R<Object> operatorPage(@RequestParam Map<String, String> q) {
        return R.ok(machineService.operatorPage(q));
    }

    @PostMapping("/operator")
    public R<Void> saveOperator(@RequestBody Requests.OperatorSave req) {
        machineService.saveOperator(req);
        return R.ok();
    }

    @GetMapping("/operator/options")
    public R<List<Operator>> operatorOptions() {
        return R.ok(machineService.operatorOptions());
    }

    /* ---------- 价目 ---------- */

    @GetMapping("/price/list")
    public R<List<ServicePrice>> priceList(@RequestParam(required = false) String keyword) {
        return R.ok(machineService.priceList(keyword));
    }

    @PostMapping("/price")
    public R<Void> savePrice(@RequestBody Requests.PriceSave req) {
        machineService.savePrice(req);
        return R.ok();
    }

    /* ---------- 地块 ---------- */

    @GetMapping("/land/page")
    public R<Object> landPage(@RequestParam Map<String, String> q) {
        return R.ok(machineService.landPage(q));
    }

    @PostMapping("/land")
    public R<Void> saveLand(@RequestBody Requests.LandSave req) {
        machineService.saveLand(req);
        return R.ok();
    }

    /* ---------- 保养维修 ---------- */

    @GetMapping("/maintain/page")
    public R<Object> maintainPage(@RequestParam Map<String, String> q) {
        return R.ok(machineService.maintainPage(q));
    }

    @PostMapping("/maintain")
    public R<Void> saveMaintain(@RequestBody Requests.MaintainSave req) {
        machineService.saveMaintain(req);
        return R.ok();
    }
}
