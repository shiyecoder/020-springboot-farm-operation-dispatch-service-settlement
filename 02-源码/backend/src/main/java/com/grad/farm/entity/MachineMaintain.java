package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 机具保养维修 */
@Data
@TableName("machine_maintain")
public class MachineMaintain {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机具 */
    private Long machineId;

    /** 机具编号 */
    private String machineNo;

    /** 类型 */
    private String maintainType;

    /** 内容 */
    private String content;

    /** 费用 */
    private BigDecimal cost;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 附件 */
    private String attachment;

    /** 经办人 */
    private String handler;

    /** 状态 */
    private String maintainStatus;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
