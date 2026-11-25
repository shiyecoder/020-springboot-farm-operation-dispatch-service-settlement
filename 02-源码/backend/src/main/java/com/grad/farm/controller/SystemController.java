package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.common.UserContext;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.SysMenu;
import com.grad.farm.entity.SysOrg;
import com.grad.farm.entity.SysRole;
import com.grad.farm.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 用户与角色、菜单授权、站内消息、操作日志。 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/users")
    public R<Object> users(@RequestParam Map<String, String> q) {
        return R.ok(systemService.userPage(q));
    }

    @PostMapping("/user")
    public R<Void> saveUser(@RequestBody Requests.UserSave req) {
        systemService.saveUser(req);
        return R.ok();
    }

    @PostMapping("/user/{id}/reset")
    public R<Void> reset(@PathVariable Long id) {
        systemService.resetPassword(id);
        return R.ok();
    }

    @GetMapping("/roles")
    public R<List<SysRole>> roles() {
        return R.ok(systemService.roles());
    }

    @GetMapping("/orgs")
    public R<List<SysOrg>> orgs() {
        return R.ok(systemService.orgs());
    }

    @GetMapping("/menus")
    public R<List<SysMenu>> menus() {
        return R.ok(systemService.menus());
    }

    @GetMapping("/role-menus")
    public R<List<Long>> roleMenus(@RequestParam String roleCode) {
        return R.ok(systemService.roleMenus(roleCode));
    }

    @PostMapping("/role-menus")
    public R<Void> saveRoleMenus(@RequestBody Requests.RoleMenuSave req) {
        systemService.saveRoleMenus(req.getRoleCode(), req.getMenuIds());
        return R.ok();
    }

    @GetMapping("/messages")
    public R<List<Map<String, Object>>> messages() {
        return R.ok(systemService.messages(UserContext.userId()));
    }

    @PostMapping("/messages/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        systemService.readMessage(id);
        return R.ok();
    }

    @GetMapping("/logs")
    public R<Object> logs(@RequestParam Map<String, String> q) {
        return R.ok(systemService.logs(q));
    }
}
