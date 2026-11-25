package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.Land;
import org.apache.ibatis.annotations.Mapper;

/** 农户地块 Mapper */
@Mapper
public interface LandMapper extends BaseMapper<Land> {
}
