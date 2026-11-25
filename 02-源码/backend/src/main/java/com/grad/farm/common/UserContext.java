package com.grad.farm.common;

/**
 * 当前登录用户上下文：由 JWT 过滤器写入，Service/Controller 只读。
 * dataScope 决定数据范围：ALL 全域 / DEPT 本部门 / SELF 本人。
 */
public class UserContext {

    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String dataScope) {}

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser u) { HOLDER.set(u); }

    public static CurrentUser get() { return HOLDER.get(); }

    public static Long userId() { CurrentUser u = HOLDER.get(); return u == null ? null : u.userId(); }

    public static String realName() { CurrentUser u = HOLDER.get(); return u == null ? "" : u.realName(); }

    public static String roleCode() { CurrentUser u = HOLDER.get(); return u == null ? "" : u.roleCode(); }

    public static Long orgId() { CurrentUser u = HOLDER.get(); return u == null ? null : u.orgId(); }

    public static String dataScope() { CurrentUser u = HOLDER.get(); return u == null ? "SELF" : u.dataScope(); }

    public static boolean isAdmin() { return hasRole("ADMIN"); }

    public static boolean hasRole(String... roles) {
        String cur = roleCode();
        for (String r : roles) {
            if (r.equalsIgnoreCase(cur)) return true;
        }
        return false;
    }

    public static void clear() { HOLDER.remove(); }
}
