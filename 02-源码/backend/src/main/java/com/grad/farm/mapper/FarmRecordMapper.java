package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.FarmRecord;
import org.apache.ibatis.annotations.Mapper;

/** 业务时间轴 Mapper */
@Mapper
public interface FarmRecordMapper extends BaseMapper<FarmRecord> {
}
