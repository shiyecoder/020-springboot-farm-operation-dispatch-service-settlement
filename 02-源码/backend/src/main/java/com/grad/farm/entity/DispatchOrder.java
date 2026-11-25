package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 作业派单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispatch_order")
public class DispatchOrder extends BaseEntity {

    /** 派单号 */
    private String orderNo;

    /** 需求 */
    private Long requestId;

    /** 需求编号 */
    private String requestNo;

    /** 关联报价 */
    private Long quotationId;

    /** 机具 */
    private Long machineId;

    /** 机具编号 */
    private String machineNo;

    /** 农机手 */
    private Long operatorId;

    /** 农机手姓名 */
    private String operatorName;

    /** 计划开始 */
    private LocalDateTime planStart;

    /** 计划结束 */
    private LocalDateTime planEnd;

    /** 计划面积 */
    private BigDecimal planArea;

    /** 状态 */
    private String dispatchStatus;

    /** 推荐评分记录 */
    private Long scoreId;

    /** 派单调度员 */
    private Long dispatcherId;

    /** 派单时间 */
    private LocalDateTime dispatchTime;

    /** 完工时间 */
    private LocalDateTime finishTime;

    /** 是否加急 */
    private Integer urgentFlag;

    /** 备注 */
    private String remark;
}
