package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 数据访问，通用 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
