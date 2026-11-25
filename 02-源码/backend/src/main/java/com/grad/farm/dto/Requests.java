package com.grad.farm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 请求对象集合：按业务场景分组，避免为每个字段单独建类。 */
public class Requests {

    @Data
    public static class Login {
        private String username;
        private String password;
    }

    @Data
    public static class Password {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class Profile {
        private String realName;
        private String phone;
        private String email;
        private String avatar;
    }

    /** 机具新增/编辑 */
    @Data
    public static class MachineSave {
        private Long id;
        private String machineNo;
        private String machineName;
        private String machineType;
        private String model;
        private BigDecimal powerHp;
        private BigDecimal workWidth;
        private LocalDate purchaseDate;
        private BigDecimal totalHours;
        private LocalDate annualCheckDate;
        private LocalDate nextMaintainDate;
        private String workTypes;
        private String region;
        private String currentStatus;
        private String coverImage;
        private BigDecimal hourlyCost;
        private BigDecimal fuelPerMu;
        private String remark;
    }

    /** 机具状态变更（保养/维修/停用/恢复） */
    @Data
    public static class MachineStatus {
        private String status;
        private String reason;
    }

    @Data
    public static class OperatorSave {
        private Long id;
        private String operatorNo;
        private String realName;
        private String phone;
        private String licenseNo;
        private LocalDate licenseExpire;
        private String certTypes;
        private LocalDate certExpire;
        private String region;
        private BigDecimal rating;
        private BigDecimal totalHours;
        private BigDecimal ontimeRate;
        private BigDecimal avgFuel;
        private BigDecimal hourlyWage;
        private String currentStatus;
        private String avatar;
        private String remark;
    }

    @Data
    public static class PriceSave {
        private Long id;
        private String serviceType;
        private String crop;
        private String unit;
        private BigDecimal basePrice;
        private BigDecimal freeKm;
        private BigDecimal roadFeePerKm;
        private BigDecimal urgentRate;
        private String priceStatus;
        private String remark;
    }

    @Data
    public static class LandSave {
        private Long id;
        private String landNo;
        private Long farmerId;
        private String landName;
        private BigDecimal area;
        private String crop;
        private String region;
        private String address;
        private BigDecimal distanceKm;
        private String image;
        private String remark;
    }

    /** 需求提交 */
    @Data
    public static class RequestSave {
        private Long id;
        private Long landId;
        private String serviceType;
        private String crop;
        private BigDecimal area;
        private LocalDate hopeDate;
        private String urgency;
        private String requireDesc;
    }

    @Data
    public static class BackReq {
        private String reason;
    }

    /** 生成报价 */
    @Data
    public static class QuoteGen {
        private Long requestId;
        private BigDecimal extraFee;
        private String remark;
    }

    /** 派单 */
    @Data
    public static class DispatchReq {
        private Long requestId;
        private Long machineId;
        private Long operatorId;
        private Long scoreId;
        private LocalDateTime planStart;
        private LocalDateTime planEnd;
        private String remark;
    }

    /** 开工 */
    @Data
    public static class WorkStart {
        private Long orderId;
        private LocalDateTime arriveTime;
        private String beforeImage;
        private String remark;
    }

    /** 完工提交 */
    @Data
    public static class WorkFinish {
        private Long orderId;
        private BigDecimal actualArea;
        private BigDecimal fuelLiters;
        private BigDecimal workHours;
        private LocalDateTime endTime;
        private String afterImage;
        private String exceptionDesc;
    }

    /** 验收 */
    @Data
    public static class AcceptReq {
        private Long orderId;
        private BigDecimal acceptArea;
        private Integer qualityScore;
        private Integer reworkFlag;
        private String reworkReason;
        private Integer secondConfirm;
        private String comment;
    }

    /** 结算登记 */
    @Data
    public static class PayReq {
        private Long id;
        private BigDecimal received;
        private String payMethod;
        private String voucherImage;
        private String remark;
    }

    /** 规则保存 */
    @Data
    public static class RuleSave {
        private Long id;
        private String ruleName;
        private String ruleVersion;
        private Map<String, BigDecimal> weights;
        private BigDecimal levelHigh;
        private BigDecimal levelMid;
        private String remark;
    }

    /** 推荐结果人工调整 */
    @Data
    public static class ScoreAdjust {
        private String finalLevel;
        private String reason;
    }

    /** 创新模块计算请求 */
    @Data
    public static class EvaluateReq {
        private Long requestId;
        private Long ruleId;
    }

    @Data
    public static class MaintainSave {
        private Long id;
        private Long machineId;
        private String maintainType;
        private String content;
        private BigDecimal cost;
        private LocalDate startDate;
        private LocalDate endDate;
        private String attachment;
        private String handler;
        private String maintainStatus;
        private String remark;
    }

    @Data
    public static class UserSave {
        private Long id;
        private String username;
        private String realName;
        private String roleCode;
        private Long orgId;
        private String phone;
        private String email;
        private String status;
        private String password;
    }

    @Data
    public static class RoleMenuSave {
        private String roleCode;
        private List<Long> menuIds;
    }
}
