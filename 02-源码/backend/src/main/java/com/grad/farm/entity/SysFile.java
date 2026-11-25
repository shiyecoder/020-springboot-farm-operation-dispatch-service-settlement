package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** sys_file：文件元数据 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 相对路径 */
    private String filePath;

    /** 大小 */
    private Long fileSize;

    /** 类型 */
    private String contentType;

    /** 上传人 */
    private String uploader;
}
