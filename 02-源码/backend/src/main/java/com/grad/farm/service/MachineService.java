package com.grad.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.farm.common.BizException;
import com.grad.farm.common.PageResult;
import com.grad.farm.common.Status;
import com.grad.farm.dto.Requests;
import com.grad.farm.entity.*;
import com.grad.farm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 资源主数据：机具档案、农机手档案、服务价目、农户地块、保养维修。
 * 主数据被业务单据引用后不可删除，只能停用。
 */
@Service
@RequiredArgsConstructor
public class MachineService {

    private final FarmMachineMapper machineMapper;
    private final OperatorMapper operatorMapper;
    private final ServicePriceMapper priceMapper;
    private final LandMapper landMapper;
    private final MachineMaintainMapper maintainMapper;
    private final DispatchOrderMapper orderMapper;
    private final FarmRecordMapper recordMapper;

    /* ==================== 机具 ==================== */

    public PageResult<Map<String, Object>> machinePage(Map<String, String> q) {
        long page = num(q.get("page"), 1), size = num(q.get("size"), 10);
        QueryWrapper<FarmMachine> w = new QueryWrapper<>();
        w.eq("deleted", 0);
        if (notBlank(q.get("keyword"))) {
            w.and(x -> x.like("machine_no", q.get("keyword")).or()
                    .like("machine_name", q.get("keyword")).or().like("model", q.get("keyword")));
        }
        if (notBlank(q.get("machineType"))) w.eq("machine_type", q.get("machineType"));
        if (notBlank(q.get("status"))) w.eq("current_status", q.get("status"));
        if (notBlank(q.get("region"))) w.eq("region", q.get("region"));
        if (notBlank(q.get("workType"))) w.like("work_types", q.get("workType"));
        w.orderByDesc("id");
        Page<FarmMachine> p = machineMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FarmMachine m : p.getRecords()) rows.add(machineVo(m));
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    private Map<String, Object> machineVo(FarmMachine m) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("id", m.getId());
        x.put("machineNo", m.getMachineNo());
        x.put("machineName", m.getMachineName());
        x.put("machineType", m.getMachineType());
        x.put("model", m.getModel());
        x.put("powerHp", m.getPowerHp());
        x.put("workWidth", m.getWorkWidth());
        x.put("purchaseDate", m.getPurchaseDate());
        x.put("totalHours", m.getTotalHours());
        x.put("annualCheckDate", m.getAnnualCheckDate());
        x.put("nextMaintainDate", m.getNextMaintainDate());
        x.put("workTypes", m.getWorkTypes());
        x.put("region", m.getRegion());
        x.put("status", m.getCurrentStatus());
        x.put("coverImage", m.getCoverImage());
        x.put("hourlyCost", m.getHourlyCost());
        x.put("fuelPerMu", m.getFuelPerMu());
        // 年检/保养临期提示：30 天内到期给出提醒，便于机务提前安排
        x.put("checkWarn", m.getAnnualCheckDate() != null
                && m.getAnnualCheckDate().isBefore(java.time.LocalDate.now().plusDays(31)));
        return x;
    }

    public Map<String, Object> machineDetail(Long id) {
        FarmMachine m = machineMapper.selectById(id);
        if (m == null) throw new BizException(404, "机具不存在");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("machine", machineVo(m));
        res.put("maintains", maintainMapper.selectList(Wrappers.<MachineMaintain>lambdaQuery()
                .eq(MachineMaintain::getMachineId, id).orderByDesc(MachineMaintain::getId)));
        res.put("recentOrders", orderMapper.selectList(Wrappers.<DispatchOrder>lambdaQuery()
                .eq(DispatchOrder::getMachineId, id).orderByDesc(DispatchOrder::getId).last("limit 8")));
        return res;
    }

    @Transactional
    public void saveMachine(Requests.MachineSave req) {
        if (!notBlank(req.getMachineNo())) throw new BizException("机具编号必填");
        if (!notBlank(req.getModel())) throw new BizException("型号必填");
        if (req.getTotalHours() == null) throw new BizException("累计工时必填");
        String no = req.getMachineNo().trim();
        Long dup = machineMapper.selectCount(Wrappers.<FarmMachine>lambdaQuery()
                .eq(FarmMachine::getMachineNo, no)
                .ne(req.getId() != null, FarmMachine::getId, req.getId()));
        if (dup != null && dup > 0) throw new BizException(409, "机具编号已存在：" + no);
        FarmMachine m = req.getId() == null ? new FarmMachine() : machineMapper.selectById(req.getId());
        if (m == null) throw new BizException(404, "机具不存在");
        m.setMachineNo(no);
        m.setMachineName(req.getMachineName());
        m.setMachineType(req.getMachineType());
        m.setModel(req.getModel());
        m.setPowerHp(req.getPowerHp());
        m.setWorkWidth(req.getWorkWidth());
        m.setPurchaseDate(req.getPurchaseDate());
        m.setTotalHours(req.getTotalHours());
        m.setAnnualCheckDate(req.getAnnualCheckDate());
        m.setNextMaintainDate(req.getNextMaintainDate());
        m.setWorkTypes(req.getWorkTypes());
        m.setRegion(req.getRegion());
        m.setCoverImage(req.getCoverImage());
        m.setHourlyCost(req.getHourlyCost());
        m.setFuelPerMu(req.getFuelPerMu());
        if (req.getId() == null) {
            m.setCurrentStatus(req.getCurrentStatus() == null ? Status.M_IDLE : req.getCurrentStatus());
            m.setDeleted(0);
            m.setVersion(0);
            machineMapper.insert(m);
        } else {
            m.setCurrentStatus(req.getCurrentStatus());
            machineMapper.updateById(m);
        }
    }

    /** 状态变更：保养/维修/停用必须填写原因，并写入时间轴 */
    @Transactional
    public void changeMachineStatus(Long id, Requests.MachineStatus req) {
        FarmMachine m = machineMapper.selectById(id);
        if (m == null) throw new BizException(404, "机具不存在");
        if (!notBlank(req.getStatus())) throw new BizException("目标状态必填");
        if (!Status.M_IDLE.equals(req.getStatus()) && !notBlank(req.getReason())) {
            throw new BizException("变更为「" + req.getStatus() + "」必须填写原因（不少于 5 个字）");
        }
        if (req.getReason() != null && !req.getReason().trim().isEmpty() && req.getReason().trim().length() < 5) {
            throw new BizException("原因不少于 5 个字");
        }
        String from = m.getCurrentStatus();
        m.setCurrentStatus(req.getStatus());
        machineMapper.updateById(m);
        FarmRecord r = new FarmRecord();
        r.setBizType("MACHINE");
        r.setBizId(id);
        r.setBizNo(m.getMachineNo());
        r.setAction("状态变更");
        r.setFromStatus(from);
        r.setToStatus(req.getStatus());
        r.setOperatorName(com.grad.farm.common.UserContext.realName());
        r.setReason(req.getReason());
        recordMapper.insert(r);
    }

    public List<FarmMachine> machineOptions() {
        return machineMapper.selectList(Wrappers.<FarmMachine>lambdaQuery()
                .eq(FarmMachine::getDeleted, 0).orderByAsc(FarmMachine::getMachineNo));
    }

    /* ==================== 农机手 ==================== */

    public PageResult<Map<String, Object>> operatorPage(Map<String, String> q) {
        long page = num(q.get("page"), 1), size = num(q.get("size"), 10);
        QueryWrapper<Operator> w = new QueryWrapper<>();
        w.eq("deleted", 0);
        if (notBlank(q.get("keyword"))) {
            w.and(x -> x.like("operator_no", q.get("keyword")).or()
                    .like("real_name", q.get("keyword")).or().like("phone", q.get("keyword")));
        }
        if (notBlank(q.get("certType"))) w.like("cert_types", q.get("certType"));
        if (notBlank(q.get("status"))) w.eq("current_status", q.get("status"));
        if (notBlank(q.get("region"))) w.eq("region", q.get("region"));
        w.orderByAsc("operator_no");
        Page<Operator> p = operatorMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Operator o : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", o.getId());
            x.put("operatorNo", o.getOperatorNo());
            x.put("realName", o.getRealName());
            x.put("phone", o.getPhone());
            x.put("licenseNo", o.getLicenseNo());
            x.put("licenseExpire", o.getLicenseExpire());
            x.put("certTypes", o.getCertTypes());
            x.put("certExpire", o.getCertExpire());
            x.put("region", o.getRegion());
            x.put("rating", o.getRating());
            x.put("totalHours", o.getTotalHours());
            x.put("ontimeRate", o.getOntimeRate());
            x.put("avgFuel", o.getAvgFuel());
            x.put("hourlyWage", o.getHourlyWage());
            x.put("status", o.getCurrentStatus());
            x.put("avatar", o.getAvatar());
            x.put("certExpired", o.getCertExpire() != null
                    && o.getCertExpire().isBefore(java.time.LocalDate.now()));
            x.put("certWarn", o.getCertExpire() != null
                    && !o.getCertExpire().isBefore(java.time.LocalDate.now())
                    && o.getCertExpire().isBefore(java.time.LocalDate.now().plusDays(60)));
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public void saveOperator(Requests.OperatorSave req) {
        if (!notBlank(req.getOperatorNo())) throw new BizException("农机手编号必填");
        if (!notBlank(req.getRealName())) throw new BizException("姓名必填");
        String no = req.getOperatorNo().trim();
        Long dup = operatorMapper.selectCount(Wrappers.<Operator>lambdaQuery()
                .eq(Operator::getOperatorNo, no)
                .ne(req.getId() != null, Operator::getId, req.getId()));
        if (dup != null && dup > 0) throw new BizException(409, "农机手编号已存在");
        Operator o = req.getId() == null ? new Operator() : operatorMapper.selectById(req.getId());
        if (o == null) throw new BizException(404, "农机手不存在");
        o.setOperatorNo(no);
        o.setRealName(req.getRealName());
        o.setPhone(req.getPhone());
        o.setLicenseNo(req.getLicenseNo());
        o.setLicenseExpire(req.getLicenseExpire());
        o.setCertTypes(req.getCertTypes());
        o.setCertExpire(req.getCertExpire());
        o.setRegion(req.getRegion());
        o.setRating(req.getRating());
        o.setTotalHours(req.getTotalHours());
        o.setOntimeRate(req.getOntimeRate());
        o.setAvgFuel(req.getAvgFuel());
        o.setHourlyWage(req.getHourlyWage());
        o.setAvatar(req.getAvatar());
        o.setRemark(req.getRemark());
        if (req.getId() == null) {
            o.setCurrentStatus(notBlank(req.getCurrentStatus()) ? req.getCurrentStatus() : "在岗");
            o.setDeleted(0);
            operatorMapper.insert(o);
        } else {
            o.setCurrentStatus(req.getCurrentStatus());
            operatorMapper.updateById(o);
        }
    }

    public List<Operator> operatorOptions() {
        return operatorMapper.selectList(Wrappers.<Operator>lambdaQuery()
                .eq(Operator::getDeleted, 0).orderByAsc(Operator::getOperatorNo));
    }

    /* ==================== 价目 ==================== */

    public List<ServicePrice> priceList(String keyword) {
        QueryWrapper<ServicePrice> w = new QueryWrapper<>();
        if (notBlank(keyword)) {
            w.and(x -> x.like("service_type", keyword).or().like("crop", keyword));
        }
        w.orderByAsc("id");
        return priceMapper.selectList(w);
    }

    @Transactional
    public void savePrice(Requests.PriceSave req) {
        if (!notBlank(req.getServiceType())) throw new BizException("作业类型必填");
        if (req.getBasePrice() == null) throw new BizException("基础单价必填");
        ServicePrice p = req.getId() == null ? new ServicePrice() : priceMapper.selectById(req.getId());
        if (p == null) throw new BizException(404, "价目不存在");
        p.setServiceType(req.getServiceType());
        p.setCrop(req.getCrop());
        p.setUnit(notBlank(req.getUnit()) ? req.getUnit() : "亩");
        p.setBasePrice(req.getBasePrice());
        p.setFreeKm(req.getFreeKm());
        p.setRoadFeePerKm(req.getRoadFeePerKm());
        p.setUrgentRate(req.getUrgentRate());
        p.setPriceStatus(notBlank(req.getPriceStatus()) ? req.getPriceStatus() : "启用");
        p.setRemark(req.getRemark());
        if (req.getId() == null) priceMapper.insert(p); else priceMapper.updateById(p);
    }

    /* ==================== 地块 ==================== */

    public PageResult<Map<String, Object>> landPage(Map<String, String> q) {
        long page = num(q.get("page"), 1), size = num(q.get("size"), 10);
        QueryWrapper<Land> w = new QueryWrapper<>();
        w.eq("deleted", 0);
        if (notBlank(q.get("keyword"))) {
            w.and(x -> x.like("land_no", q.get("keyword")).or().like("land_name", q.get("keyword")));
        }
        if (notBlank(q.get("crop"))) w.eq("crop", q.get("crop"));
        // 数据范围：农户只看自己的地块
        if ("SELF".equals(com.grad.farm.common.UserContext.dataScope())) {
            w.eq("farmer_id", com.grad.farm.common.UserContext.userId());
        }
        w.orderByDesc("id");
        Page<Land> p = landMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Land l : p.getRecords()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", l.getId());
            x.put("landNo", l.getLandNo());
            x.put("farmerId", l.getFarmerId());
            x.put("landName", l.getLandName());
            x.put("area", l.getArea());
            x.put("crop", l.getCrop());
            x.put("region", l.getRegion());
            x.put("address", l.getAddress());
            x.put("distanceKm", l.getDistanceKm());
            x.put("image", l.getImage());
            rows.add(x);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public void saveLand(Requests.LandSave req) {
        if (!notBlank(req.getLandName())) throw new BizException("地块名称必填");
        if (req.getArea() == null || req.getArea().doubleValue() <= 0) throw new BizException("面积必须大于 0");
        Land l = req.getId() == null ? new Land() : landMapper.selectById(req.getId());
        if (l == null) throw new BizException(404, "地块不存在");
        l.setLandName(req.getLandName());
        l.setArea(req.getArea());
        l.setCrop(req.getCrop());
        l.setRegion(req.getRegion());
        l.setAddress(req.getAddress());
        l.setDistanceKm(req.getDistanceKm());
        l.setImage(req.getImage());
        l.setRemark(req.getRemark());
        if (req.getId() == null) {
            Long uid = com.grad.farm.common.UserContext.userId();
            l.setFarmerId(req.getFarmerId() == null ? uid : req.getFarmerId());
            l.setLandNo("DK-" + String.format("%03d", (landMapper.selectCount(null) + 1)));
            l.setDeleted(0);
            landMapper.insert(l);
        } else {
            landMapper.updateById(l);
        }
    }

    /* ==================== 保养维修 ==================== */

    public PageResult<MachineMaintain> maintainPage(Map<String, String> q) {
        long page = num(q.get("page"), 1), size = num(q.get("size"), 10);
        QueryWrapper<MachineMaintain> w = new QueryWrapper<>();
        if (notBlank(q.get("keyword"))) w.like("machine_no", q.get("keyword"));
        if (notBlank(q.get("type"))) w.eq("maintain_type", q.get("type"));
        if (notBlank(q.get("status"))) w.eq("maintain_status", q.get("status"));
        w.orderByDesc("id");
        Page<MachineMaintain> p = maintainMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    @Transactional
    public void saveMaintain(Requests.MaintainSave req) {
        if (req.getMachineId() == null) throw new BizException("机具必填");
        FarmMachine m = machineMapper.selectById(req.getMachineId());
        if (m == null) throw new BizException(404, "机具不存在");
        MachineMaintain mt = req.getId() == null ? new MachineMaintain() : maintainMapper.selectById(req.getId());
        if (mt == null) throw new BizException(404, "记录不存在");
        mt.setMachineId(req.getMachineId());
        mt.setMachineNo(m.getMachineNo());
        mt.setMaintainType(req.getMaintainType());
        mt.setContent(req.getContent());
        mt.setCost(req.getCost());
        mt.setStartDate(req.getStartDate());
        mt.setEndDate(req.getEndDate());
        mt.setAttachment(req.getAttachment());
        mt.setHandler(notBlank(req.getHandler()) ? req.getHandler() : com.grad.farm.common.UserContext.realName());
        mt.setMaintainStatus(notBlank(req.getMaintainStatus()) ? req.getMaintainStatus() : "进行中");
        mt.setRemark(req.getRemark());
        if (req.getId() == null) maintainMapper.insert(mt); else maintainMapper.updateById(mt);
        // 进行中的保养/维修自动把机具置为不可派单状态，结束后恢复空闲
        if ("进行中".equals(mt.getMaintainStatus())) {
            m.setCurrentStatus("维修".equals(mt.getMaintainType()) ? Status.M_REPAIR : Status.M_MAINTAIN);
            machineMapper.updateById(m);
        } else if (Status.M_MAINTAIN.equals(m.getCurrentStatus()) || Status.M_REPAIR.equals(m.getCurrentStatus())) {
            m.setCurrentStatus(Status.M_IDLE);
            m.setNextMaintainDate(LocalDateTime.now().toLocalDate().plusMonths(6));
            machineMapper.updateById(m);
        }
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private static long num(String s, long def) {
        try { return s == null ? def : Long.parseLong(s); } catch (Exception e) { return def; }
    }
}
