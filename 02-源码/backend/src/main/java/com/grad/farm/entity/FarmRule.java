package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 派单推荐规则 */
@Data
@TableName("farm_rule")
public class FarmRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 版本号 */
    private String ruleVersion;

    /** 能力权重 */
    private BigDecimal wCapability;

    /** 资质权重 */
    private BigDecimal wCert;

    /** 空闲权重 */
    private BigDecimal wFree;

    /** 距离权重 */
    private BigDecimal wDistance;

    /** 油耗权重 */
    private BigDecimal wFuel;

    /** 准时率权重 */
    private BigDecimal wOntime;

    /** 推荐阈值 */
    private BigDecimal levelHigh;

    /** 可选阈值 */
    private BigDecimal levelMid;

    /** 状态 */
    private String ruleStatus;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
