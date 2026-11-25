package com.grad.farm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grad.farm.common.BizException;
import com.grad.farm.common.JwtUtil;
import com.grad.farm.common.UserContext;
import com.grad.farm.entity.SysMenu;
import com.grad.farm.entity.SysUser;
import com.grad.farm.mapper.SysMenuMapper;
import com.grad.farm.mapper.SysRoleMenuMapper;
import com.grad.farm.mapper.SysUserMapper;
import com.grad.farm.entity.SysRoleMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证与个人中心。
 * 登录连续失败 5 次锁定 10 分钟；任何返回体都不含密码字段。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAIL = 5;

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BizException("账号和密码不能为空");
        }
        SysUser u = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username.trim()).last("limit 1"));
        if (u == null) {
            throw new BizException(401, "账号或密码错误");
        }
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(423, "账号已锁定，请于 " + u.getLockedUntil() + " 后重试");
        }
        if ("停用".equals(u.getStatus())) {
            throw new BizException(403, "账号已停用，请联系系统管理员");
        }
        if (!encoder.matches(password, u.getPassword())) {
            int fail = (u.getLoginFail() == null ? 0 : u.getLoginFail()) + 1;
            u.setLoginFail(fail);
            if (fail >= MAX_FAIL) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(10));
                u.setLoginFail(0);
            }
            userMapper.updateById(u);
            throw new BizException(401, "账号或密码错误" + (fail >= MAX_FAIL ? "，已锁定 10 分钟" : "，剩余 " + (MAX_FAIL - fail) + " 次"));
        }
        u.setLoginFail(0);
        u.setLockedUntil(null);
        userMapper.updateById(u);

        UserContext.CurrentUser cu = new UserContext.CurrentUser(u.getId(), u.getUsername(), u.getRealName(),
                u.getRoleCode(), u.getOrgId(), u.getDataScope());
        Map<String, Object> res = new HashMap<>();
        res.put("token", jwtUtil.create(cu));
        res.put("user", profile(u));
        return res;
    }

    /** 当前用户信息 + 菜单（按角色动态下发，前端据此生成侧边栏） */
    public Map<String, Object> me() {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "用户不存在");
        }
        Map<String, Object> res = new HashMap<>();
        res.put("user", profile(u));
        res.put("menus", menus(u.getRoleCode()));
        return res;
    }

    public List<SysMenu> menus(String roleCode) {
        List<Long> ids = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleCode, roleCode))
                .stream().map(SysRoleMenu::getMenuId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getId, ids).eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSort));
    }

    private Map<String, Object> profile(SysUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("avatar", u.getAvatar());
        m.put("roleCode", u.getRoleCode());
        m.put("orgId", u.getOrgId());
        m.put("phone", u.getPhone());
        m.put("email", u.getEmail());
        m.put("dataScope", u.getDataScope());
        m.put("status", u.getStatus());
        return m;
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null || !encoder.matches(oldPassword, u.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码不少于 6 位");
        }
        u.setPassword(encoder.encode(newPassword));
        userMapper.updateById(u);
    }

    public void updateProfile(String realName, String phone, String email, String avatar) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException("用户不存在");
        }
        if (realName != null && !realName.isBlank()) {
            u.setRealName(realName.trim());
        }
        u.setPhone(phone);
        u.setEmail(email);
        if (avatar != null && !avatar.isBlank()) {
            u.setAvatar(avatar);
        }
        userMapper.updateById(u);
    }
}
