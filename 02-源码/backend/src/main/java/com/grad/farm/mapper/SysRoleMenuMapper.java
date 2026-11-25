package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/** 角色菜单 数据访问，通用 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
