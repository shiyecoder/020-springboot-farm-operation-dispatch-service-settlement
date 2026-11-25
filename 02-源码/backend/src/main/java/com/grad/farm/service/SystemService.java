package com.grad.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.farm.common.BizException;
import com.grad.farm.common.PageResult;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.*;
import com.grad.farm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 系统管理：用户、角色、菜单、角色菜单授权、站内消息与操作日志。 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMessageMapper messageMapper;
    private final SysLogMapper logMapper;
    private final SysOrgMapper orgMapper;
    private final PasswordEncoder encoder;

    public PageResult<Map<String, Object>> userPage(Map<String, String> q) {
        long page = q.get("page") == null ? 1 : Long.parseLong(q.get("page"));
        long size = q.get("size") == null ? 10 : Long.parseLong(q.get("size"));
        QueryWrapper<SysUser> w = new QueryWrapper<>();
        w.eq("deleted", 0);
        if (q.get("keyword") != null && !q.get("keyword").isBlank()) {
            w.and(x -> x.like("username", q.get("keyword")).or().like("real_name", q.get("keyword")));
        }
        if (q.get("roleCode") != null && !q.get("roleCode").isBlank()) {
            w.eq("role_code", q.get("roleCode"));
        }
        w.orderByAsc("id");
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser u : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", u.getId());
            x.put("username", u.getUsername());
            x.put("realName", u.getRealName());
            x.put("avatar", u.getAvatar());
            x.put("roleCode", u.getRoleCode());
            x.put("orgId", u.getOrgId());
            x.put("phone", u.getPhone());
            x.put("email", u.getEmail());
            x.put("dataScope", u.getDataScope());
            x.put("status", u.getStatus());
            x.put("createTime", u.getCreateTime());
            SysOrg o = u.getOrgId() == null ? null : orgMapper.selectById(u.getOrgId());
            x.put("orgName", o == null ? "—" : o.getOrgName());
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public void saveUser(Requests.UserSave req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new BizException("账号必填");
        }
        if (req.getId() == null) {
            Long dup = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, req.getUsername().trim()));
            if (dup != null && dup > 0) {
                throw new BizException(409, "账号已存在");
            }
            SysUser u = new SysUser();
            u.setUsername(req.getUsername().trim());
            u.setPassword(encoder.encode(req.getPassword() == null || req.getPassword().isBlank()
                    ? "123456" : req.getPassword()));
            u.setRealName(req.getRealName());
            u.setRoleCode(req.getRoleCode());
            u.setOrgId(req.getOrgId());
            u.setPhone(req.getPhone());
            u.setEmail(req.getEmail());
            u.setStatus(req.getStatus() == null ? "启用" : req.getStatus());
            u.setDataScope(scopeOf(req.getRoleCode()));
            u.setLoginFail(0);
            userMapper.insert(u);
        } else {
            SysUser u = userMapper.selectById(req.getId());
            if (u == null) {
                throw new BizException(404, "用户不存在");
            }
            u.setRealName(req.getRealName());
            u.setRoleCode(req.getRoleCode());
            u.setOrgId(req.getOrgId());
            u.setPhone(req.getPhone());
            u.setEmail(req.getEmail());
            u.setStatus(req.getStatus());
            u.setDataScope(scopeOf(req.getRoleCode()));
            userMapper.updateById(u);
        }
    }

    public void resetPassword(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(404, "用户不存在");
        }
        u.setPassword(encoder.encode("123456"));
        u.setLoginFail(0);
        u.setLockedUntil(null);
        userMapper.updateById(u);
    }

    private String scopeOf(String roleCode) {
        return switch (roleCode == null ? "" : roleCode) {
            case "ADMIN", "DISPATCHER", "MAINT", "FINANCE" -> "ALL";
            case "OPERATOR", "FARMER" -> "SELF";
            default -> "SELF";
        };
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
    }

    public List<SysOrg> orgs() {
        return orgMapper.selectList(Wrappers.<SysOrg>lambdaQuery().orderByAsc(SysOrg::getId));
    }

    public List<SysMenu> menus() {
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSort));
    }

    public List<Long> roleMenus(String roleCode) {
        return roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleCode, roleCode))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Transactional
    public void saveRoleMenus(String roleCode, List<Long> menuIds) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BizException("角色必填");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleCode, roleCode));
        if (menuIds == null) {
            return;
        }
        for (Long id : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleCode(roleCode);
            rm.setMenuId(id);
            roleMenuMapper.insert(rm);
        }
    }

    public List<Map<String, Object>> messages(Long userId) {
        List<SysMessage> list = messageMapper.selectList(Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getReceiverId, userId)
                .orderByDesc(SysMessage::getId).last("limit 30"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysMessage m : list) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", m.getId());
            x.put("title", m.getTitle());
            x.put("content", m.getContent());
            x.put("bizNo", m.getBizNo());
            x.put("bizType", m.getBizType());
            x.put("isRead", m.getIsRead());
            x.put("createTime", m.getCreateTime());
            rows.add(x);
        }
        return rows;
    }

    public void readMessage(Long id) {
        SysMessage m = messageMapper.selectById(id);
        if (m != null) {
            m.setIsRead(1);
            messageMapper.updateById(m);
        }
    }

    public PageResult<Map<String, Object>> logs(Map<String, String> q) {
        long page = q.get("page") == null ? 1 : Long.parseLong(q.get("page"));
        long size = q.get("size") == null ? 10 : Long.parseLong(q.get("size"));
        Page<SysLog> p = logMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<SysLog>().orderByDesc("id"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysLog l : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", l.getId());
            x.put("username", l.getUsername());
            x.put("module", l.getModule());
            x.put("action", l.getAction());
            x.put("method", l.getMethod());
            x.put("ip", l.getIp());
            x.put("costMs", l.getCostMs());
            x.put("result", l.getResult());
            x.put("createTime", l.getCreateTime());
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }
}
