package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 农机档案 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("farm_machine")
public class FarmMachine extends BaseEntity {

    /** 机具编号 */
    private String machineNo;

    /** 机具名称 */
    private String machineName;

    /** 机具类型 */
    private String machineType;

    /** 型号 */
    private String model;

    /** 功率（马力） */
    private BigDecimal powerHp;

    /** 作业宽度（米） */
    private BigDecimal workWidth;

    /** 购置日期 */
    private LocalDate purchaseDate;

    /** 累计工时 */
    private BigDecimal totalHours;

    /** 年检日期 */
    private LocalDate annualCheckDate;

    /** 下次保养日期 */
    private LocalDate nextMaintainDate;

    /** 可作业类型 */
    private String workTypes;

    /** 当前区域 */
    private String region;

    /** 可用状态 */
    private String currentStatus;

    /** 机具照片 */
    private String coverImage;

    /** 台班成本 */
    private BigDecimal hourlyCost;

    /** 亩均油耗 */
    private BigDecimal fuelPerMu;

    /** 乐观锁版本号 */
    private Integer version;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建人 */
    private String createBy;
}
