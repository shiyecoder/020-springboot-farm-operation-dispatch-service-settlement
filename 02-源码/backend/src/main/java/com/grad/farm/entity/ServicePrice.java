package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 服务价目 */
@Data
@TableName("service_price")
public class ServicePrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业类型 */
    private String serviceType;

    /** 作物 */
    private String crop;

    /** 计费单位 */
    private String unit;

    /** 基础单价 */
    private BigDecimal basePrice;

    /** 免费里程 */
    private BigDecimal freeKm;

    /** 超里程单价 */
    private BigDecimal roadFeePerKm;

    /** 加急系数 */
    private BigDecimal urgentRate;

    /** 状态 */
    private String priceStatus;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
