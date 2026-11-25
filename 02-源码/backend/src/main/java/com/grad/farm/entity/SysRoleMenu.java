package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** sys_role_menu：角色菜单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
public class SysRoleMenu extends BaseEntity {

    /** 角色编码 */
    private String roleCode;

    /** 菜单ID */
    private Long menuId;
}
