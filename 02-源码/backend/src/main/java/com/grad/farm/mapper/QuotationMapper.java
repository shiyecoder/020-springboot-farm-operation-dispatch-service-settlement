package com.grad.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.farm.entity.Quotation;
import org.apache.ibatis.annotations.Mapper;

/** 报价单 Mapper */
@Mapper
public interface QuotationMapper extends BaseMapper<Quotation> {
}
