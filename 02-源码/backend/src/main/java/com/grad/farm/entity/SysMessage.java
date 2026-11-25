package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** sys_message：站内消息 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务编号 */
    private String bizNo;

    /** 业务类型 */
    private String bizType;

    /** 接收人 */
    private Long receiverId;

    /** 跳转地址 */
    private String url;

    /** 是否已读 */
    private Integer isRead;
}
