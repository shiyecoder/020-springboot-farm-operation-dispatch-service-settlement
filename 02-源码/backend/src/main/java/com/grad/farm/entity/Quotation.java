package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 报价单 */
@Data
@TableName("quotation")
public class Quotation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报价单号 */
    private String quotationNo;

    /** 需求 */
    private Long requestId;

    /** 需求编号 */
    private String requestNo;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 计价面积 */
    private BigDecimal area;

    /** 基础作业费 */
    private BigDecimal baseAmount;

    /** 路程附加费 */
    private BigDecimal roadFee;

    /** 加急附加费 */
    private BigDecimal urgentFee;

    /** 其他附加费 */
    private BigDecimal extraFee;

    /** 报价合计 */
    private BigDecimal totalAmount;

    /** 状态 */
    private String quoteStatus;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 有效期至 */
    private LocalDateTime expireTime;

    /** 备注 */
    private String remark;

    /** 制单人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
