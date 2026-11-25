package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 验收记录 */
@Data
@TableName("acceptance")
public class Acceptance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 派单 */
    private Long orderId;

    /** 派单号 */
    private String orderNo;

    /** 验收农户 */
    private Long farmerId;

    /** 确认面积 */
    private BigDecimal acceptArea;

    /** 质量评分 */
    private Integer qualityScore;

    /** 是否返工 */
    private Integer reworkFlag;

    /** 返工原因 */
    private String reworkReason;

    /** 面积偏差率 */
    private BigDecimal areaDiffRate;

    /** 二次确认 */
    private Integer secondConfirm;

    /** 超时验收 */
    private Integer overdueFlag;

    /** 评价 */
    private String comment;

    /** 验收时间 */
    private LocalDateTime acceptTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
