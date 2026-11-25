package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 结算单 */
@Data
@TableName("settlement")
public class Settlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 结算单号 */
    private String settleNo;

    /** 派单 */
    private Long orderId;

    /** 派单号 */
    private String orderNo;

    /** 农户 */
    private Long farmerId;

    /** 应收 */
    private BigDecimal receivable;

    /** 已收 */
    private BigDecimal received;

    /** 人工调整 */
    private BigDecimal adjustAmount;

    /** 状态 */
    private String settleStatus;

    /** 收款方式 */
    private String payMethod;

    /** 凭证 */
    private String voucherImage;

    /** 结算时间 */
    private LocalDateTime settleTime;

    /** 财务经办人 */
    private Long financeId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
