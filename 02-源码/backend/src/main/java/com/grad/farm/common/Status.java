package com.grad.farm.common;

import java.util.List;

/**
 * 农机作业派单系统的状态机与业务常量。
 *
 * 主流程（需求单据）：
 *   待报价 → 待确认 → 待排程 → 待作业 → 作业中 → 待验收 → 已完成
 * 分支：已退回（调度员退回需求，农户修改后重新提交）、已取消（农户主动撤回）；
 *      作业中断（现场异常，恢复后继续作业）；待返工（验收不合格，重新排程）。
 *
 * 后端强制校验（不依赖前端隐藏按钮）：
 *  1. 机具处于「保养中 / 维修中 / 停用」时不得派单；
 *  2. 农机手操作证在计划作业日期前到期或已过期时不得派单；
 *  3. 同一机具、同一农机手的计划时段不得重叠（状态为待作业 / 作业中 / 待验收）；
 *  4. 实际面积与报价面积偏差超过 3% 时，必须由农户二次确认后才可结算；
 *  5. 已收款的订单不可再修改金额，减免只记录线下调整单；
 *  6. 状态流转必须按顺序推进，越级调用直接拒绝。
 */
public final class Status {

    /* ---------------- 需求 / 派单状态 ---------------- */
    public static final String QUOTING = "待报价";
    public static final String CONFIRMING = "待确认";
    public static final String SCHEDULING = "待排程";
    public static final String WAIT_WORK = "待作业";
    public static final String WORKING = "作业中";
    public static final String WAIT_ACCEPT = "待验收";
    public static final String FINISHED = "已完成";
    public static final String RETURNED = "已退回";
    public static final String CANCELLED = "已取消";
    public static final String REWORK = "待返工";

    /* ---------------- 机具状态 ---------------- */
    public static final String M_IDLE = "空闲";
    public static final String M_WORKING = "作业中";
    public static final String M_MAINTAIN = "保养中";
    public static final String M_REPAIR = "维修中";
    public static final String M_DISABLED = "停用";

    /* ---------------- 结算状态 ---------------- */
    public static final String S_UNPAID = "待收款";
    public static final String S_PART = "部分收款";
    public static final String S_PAID = "已收款";

    /* ---------------- 推荐等级 ---------------- */
    public static final String LV_HIGH = "推荐";
    public static final String LV_MID = "可选";
    public static final String LV_LOW = "不推荐";

    /** 面积偏差阈值（%），超过必须农户二次确认 */
    public static final double AREA_DIFF_THRESHOLD = 3.0;

    private Status() {}

    /** 需求单据可流转的下一状态 */
    public static List<String> nextOf(String current) {
        return switch (current == null ? "" : current) {
            case QUOTING -> List.of(CONFIRMING, RETURNED, CANCELLED);
            case CONFIRMING -> List.of(SCHEDULING, RETURNED, CANCELLED);
            case SCHEDULING -> List.of(WAIT_WORK, RETURNED);
            case WAIT_WORK -> List.of(WORKING);
            case WORKING -> List.of(WAIT_ACCEPT);
            case WAIT_ACCEPT -> List.of(FINISHED, REWORK);
            case REWORK -> List.of(SCHEDULING);
            default -> List.of();
        };
    }

    public static boolean canTransit(String from, String to) {
        return nextOf(from).contains(to);
    }

    /** 机具是否可派单 */
    public static boolean machineDispatchable(String machineStatus) {
        return M_IDLE.equals(machineStatus) || M_WORKING.equals(machineStatus);
    }

    public static String levelOf(double total, double high, double mid) {
        if (total >= high) return LV_HIGH;
        if (total >= mid) return LV_MID;
        return LV_LOW;
    }
}
