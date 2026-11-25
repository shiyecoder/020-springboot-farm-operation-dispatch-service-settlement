package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

/** 菜单 数据访问，通用 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}
