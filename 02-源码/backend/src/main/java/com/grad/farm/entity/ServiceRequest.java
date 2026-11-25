package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 作业需求 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_request")
public class ServiceRequest extends BaseEntity {

    /** 需求编号 */
    private String requestNo;

    /** 农户 */
    private Long farmerId;

    /** 农户姓名 */
    private String farmerName;

    /** 地块 */
    private Long landId;

    /** 地块名称 */
    private String landName;

    /** 服务类型 */
    private String serviceType;

    /** 作物 */
    private String crop;

    /** 面积 */
    private BigDecimal area;

    /** 希望作业日期 */
    private LocalDate hopeDate;

    /** 紧急度 */
    private String urgency;

    /** 特殊要求 */
    private String requireDesc;

    /** 状态 */
    private String requestStatus;

    /** 负责调度员 */
    private Long dispatcherId;

    /** 退回原因 */
    private String backReason;

    /** 确认金额 */
    private BigDecimal totalAmount;
}
