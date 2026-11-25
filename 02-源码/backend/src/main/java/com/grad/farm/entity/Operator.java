package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 农机手档案 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operator")
public class Operator extends BaseEntity {

    /** 编号 */
    private String operatorNo;

    /** 姓名 */
    private String realName;

    /** 电话 */
    private String phone;

    /** 登录账号 */
    private Long userId;

    /** 驾驶证号 */
    private String licenseNo;

    /** 驾驶证有效期 */
    private LocalDate licenseExpire;

    /** 可驾机型 */
    private String certTypes;

    /** 操作证有效期 */
    private LocalDate certExpire;

    /** 常驻区域 */
    private String region;

    /** 评分 */
    private BigDecimal rating;

    /** 累计工时 */
    private BigDecimal totalHours;

    /** 历史准时率 */
    private BigDecimal ontimeRate;

    /** 历史亩均油耗 */
    private BigDecimal avgFuel;

    /** 小时工资 */
    private BigDecimal hourlyWage;

    /** 状态 */
    private String currentStatus;

    /** 头像 */
    private String avatar;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    private Integer deleted;
}
