package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.WorkRecord;
import org.apache.ibatis.annotations.Mapper;

/** 作业执行记录 Mapper */
@Mapper
public interface WorkRecordMapper extends BaseMapper<WorkRecord> {
}
