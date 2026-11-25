package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.FarmScore;
import org.apache.ibatis.annotations.Mapper;

/** 派单推荐计算留痕 Mapper */
@Mapper
public interface FarmScoreMapper extends BaseMapper<FarmScore> {
}
