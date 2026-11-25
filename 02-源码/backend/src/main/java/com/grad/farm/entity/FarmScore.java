package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 派单推荐计算留痕 */
@Data
@TableName("farm_score")
public class FarmScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则 */
    private Long ruleId;

    /** 规则版本 */
    private String ruleVersion;

    /** 需求 */
    private Long requestId;

    /** 需求编号 */
    private String requestNo;

    /** 机具 */
    private Long machineId;

    /** 机具编号 */
    private String machineNo;

    /** 农机手 */
    private Long operatorId;

    /** 农机手姓名 */
    private String operatorName;

    /** 能力得分 */
    private BigDecimal capabilityScore;

    /** 资质得分 */
    private BigDecimal certScore;

    /** 空闲得分 */
    private BigDecimal freeScore;

    /** 距离得分 */
    private BigDecimal distanceScore;

    /** 油耗得分 */
    private BigDecimal fuelScore;

    /** 准时率得分 */
    private BigDecimal ontimeScore;

    /** 总分 */
    private BigDecimal totalScore;

    /** 推荐结果 */
    private String resultLevel;

    /** 最终等级 */
    private String finalLevel;

    /** 主要影响因素 */
    private String explainText;

    /** 未满足条件 */
    private String unmetText;

    /** 可改进项 */
    private String improveText;

    /** 输入快照 */
    private String snapshot;

    /** 调整原因 */
    private String adjustReason;

    /** 调整人 */
    private String adjustBy;

    /** 调整时间 */
    private LocalDateTime adjustTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
