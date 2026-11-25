package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 作业执行记录 */
@Data
@TableName("work_record")
public class WorkRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 派单 */
    private Long orderId;

    /** 派单号 */
    private String orderNo;

    /** 农机手 */
    private Long operatorId;

    /** 到场时间 */
    private LocalDateTime arriveTime;

    /** 开工时间 */
    private LocalDateTime startTime;

    /** 完工时间 */
    private LocalDateTime endTime;

    /** 实际面积 */
    private BigDecimal actualArea;

    /** 油耗 */
    private BigDecimal fuelLiters;

    /** 工时 */
    private BigDecimal workHours;

    /** 作业前照片 */
    private String beforeImage;

    /** 作业后照片 */
    private String afterImage;

    /** 异常说明 */
    private String exceptionDesc;

    /** 记录状态 */
    private String recordStatus;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
