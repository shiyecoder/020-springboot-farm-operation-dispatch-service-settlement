package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/** 业务时间轴 */
@Data
@TableName("farm_record")
public class FarmRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 动作 */
    private String action;

    /** 原状态 */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 操作人 */
    private String operatorName;

    /** 原因 */
    private String reason;

    /** 创建时间 */
    private LocalDateTime createTime;
}
