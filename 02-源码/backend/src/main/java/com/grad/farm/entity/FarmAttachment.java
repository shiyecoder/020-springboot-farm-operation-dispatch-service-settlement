package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/** 业务附件 */
@Data
@TableName("farm_attachment")
public class FarmAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 文件名 */
    private String fileName;

    /** 相对路径 */
    private String filePath;

    /** 大小 */
    private Long fileSize;

    /** 上传人 */
    private String uploadBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
