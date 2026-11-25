package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/** 站内消息 数据访问，通用 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
