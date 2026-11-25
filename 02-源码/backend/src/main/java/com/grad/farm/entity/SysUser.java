package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/** sys_user：用户 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号 */
    private String username;

    /** BCrypt 密码 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 头像相对路径 */
    private String avatar;

    /** 角色编码 */
    private String roleCode;

    /** 所属组织 */
    private Long orgId;

    /** 手机 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 数据范围 */
    private String dataScope;

    /** 启用/停用 */
    private String status;

    /** 连续失败次数 */
    private Integer loginFail;

    /** 锁定到期时间 */
    private LocalDateTime lockedUntil;
}
