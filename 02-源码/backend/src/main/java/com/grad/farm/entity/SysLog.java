package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** sys_log：操作日志 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLog extends BaseEntity {

    /** 操作人 */
    private Long userId;

    /** 账号 */
    private String username;

    /** 模块 */
    private String module;

    /** 动作 */
    private String action;

    /** 请求方法与路径 */
    private String method;

    /** 参数摘要 */
    private String params;

    /** IP */
    private String ip;

    /** 耗时ms */
    private Long costMs;

    /** 结果 */
    private String result;
}
