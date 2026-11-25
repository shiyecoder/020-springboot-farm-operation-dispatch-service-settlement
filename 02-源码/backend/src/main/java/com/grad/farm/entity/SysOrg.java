package com.grad.farm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** sys_org：组织（院区/科室/用能部门） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {

    /** 组织编码 */
    private String orgCode;

    /** 组织名称 */
    private String orgName;

    /** 上级组织 */
    private Long parentId;

    /** ORG/CAMPUS/DEPT */
    private String orgType;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 排序 */
    private Integer sort;

    /** 状态 */
    private String status;
}
