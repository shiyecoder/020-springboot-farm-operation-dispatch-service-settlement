package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 农户地块 */
@Data
@TableName("land")
public class Land {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 地块编号 */
    private String landNo;

    /** 所属农户 */
    private Long farmerId;

    /** 地块名称 */
    private String landName;

    /** 面积（亩） */
    private BigDecimal area;

    /** 当季作物 */
    private String crop;

    /** 区域 */
    private String region;

    /** 位置 */
    private String address;

    /** 距合作社里程 */
    private BigDecimal distanceKm;

    /** 地块照片 */
    private String image;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}
