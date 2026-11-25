package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** sys_menu：菜单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 上级菜单 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 路由路径 */
    private String path;

    /** 前端组件 */
    private String component;

    /** 图标 */
    private String icon;

    /** 权限标识 */
    private String perm;

    /** M/C/B */
    private String menuType;

    /** 排序 */
    private Integer sort;

    /** 是否显示 */
    private Integer visible;
}
