package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.DispatchOrder;
import org.apache.ibatis.annotations.Mapper;

/** 作业派单 Mapper */
@Mapper
public interface DispatchOrderMapper extends BaseMapper<DispatchOrder> {
}
