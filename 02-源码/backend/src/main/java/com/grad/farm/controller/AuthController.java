package com.grad.farm.controller;

import com.grad.farm.common.R;
import com.grad.farm.dto.Requests;
import com.grad.farm.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 登录、当前用户、改密、改资料。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Requests.Login req) {
        return R.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.me());
    }

    @PostMapping("/password")
    public R<Void> password(@RequestBody Requests.Password req) {
        authService.changePassword(req.getOldPassword(), req.getNewPassword());
        return R.ok();
    }

    @PutMapping("/profile")
    public R<Void> profile(@RequestBody Requests.Profile req) {
        authService.updateProfile(req.getRealName(), req.getPhone(), req.getEmail(), req.getAvatar());
        return R.ok();
    }
}
