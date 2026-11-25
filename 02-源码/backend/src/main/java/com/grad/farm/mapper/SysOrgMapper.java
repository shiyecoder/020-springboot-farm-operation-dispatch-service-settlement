package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;

/** 组织（院区/科室/用能部门） 数据访问，通用 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {
}
