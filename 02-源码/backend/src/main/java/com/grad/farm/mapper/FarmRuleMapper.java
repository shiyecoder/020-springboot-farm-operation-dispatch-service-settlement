package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.FarmRule;
import org.apache.ibatis.annotations.Mapper;

/** 派单推荐规则 Mapper */
@Mapper
public interface FarmRuleMapper extends BaseMapper<FarmRule> {
}
