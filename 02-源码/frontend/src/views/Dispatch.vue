<template>
  <div>
    <div class="page-head">
      <div>
        <h2>智能排程台</h2>
        <div class="sub">先校验资质与机具能力，再按空闲时段、到场距离、油耗与准时率加权推荐；派单前必须通过硬性校验</div>
      </div>
      <div class="rule-chip" v-if="rule">
        当前规则 {{ rule.ruleVersion }} · 推荐≥{{ rule.levelHigh }} / 可选≥{{ rule.levelMid }}
        <el-button link type="primary" @click="$router.push('/rule')">调整权重</el-button>
      </div>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-select v-model="requestId" placeholder="选择待排程需求" filterable style="width: 360px" @change="loadCandidates">
          <el-option v-for="r in queue" :key="r.id"
                     :label="`${r.requestNo}｜${r.farmerName}｜${r.serviceType} ${r.area}亩｜${r.hopeDate}`" :value="r.id" />
        </el-select>
        <el-button type="primary" :icon="Cpu" :disabled="!requestId" :loading="evaluating" @click="evaluate">
          正式计算并留痕
        </el-button>
        <span class="tip">试算不落库，点「正式计算」会保存输入快照与各项得分</span>
      </div>
    </div>

    <div v-if="current" class="req-strip">
      <span><b>{{ current.requestNo }}</b></span>
      <span>{{ current.farmerName }} · {{ current.landName }}</span>
      <span>{{ current.serviceType }} / {{ current.crop }}</span>
      <span>{{ current.area }} 亩</span>
      <span>希望 {{ current.hopeDate }}</span>
      <el-tag size="small" :type="current.urgency === '普通' ? 'info' : 'warning'">{{ current.urgency }}</el-tag>
    </div>

    <div v-if="candidates.length" class="cand-grid">
      <div v-for="(c, i) in candidates.slice(0, 3)" :key="c.machineId" class="cand-card" :class="{ best: i === 0 }">
        <div class="cand-head">
          <div>
            <div class="cand-title">
              {{ c.machineName }}
              <el-tag v-if="i === 0" size="small" type="success" effect="dark">最优</el-tag>
            </div>
            <div class="cand-sub">{{ c.machineNo }} · {{ c.operatorName }}<span v-if="c.operatorPhone"> · {{ c.operatorPhone }}</span></div>
          </div>
          <div class="cand-score">
            <div class="score-value">{{ c.totalScore }}</div>
            <el-tag size="small" :type="levelType(c.level)">{{ c.level }}</el-tag>
          </div>
        </div>

        <div class="factors">
          <div v-for="f in c.scores" :key="f.key" class="factor">
            <div class="factor-label">
              <span>{{ f.name }}<em>{{ f.weight }}%</em></span>
              <b>{{ f.score }}</b>
            </div>
            <div class="bar"><i :style="{ width: f.score + '%', background: barColor(f.score) }"></i></div>
          </div>
        </div>

        <div class="explain">
          <p><b>主要影响因素：</b>{{ c.explain }}</p>
          <p><b>未满足条件：</b><span :class="{ warn: c.unmet !== '无' }">{{ c.unmet }}</span></p>
          <p><b>可改进项：</b>{{ c.improve }}</p>
        </div>

        <div class="cand-actions">
          <el-button type="primary" size="small" @click="openDispatch(c)">按此组合派单</el-button>
          <el-button size="small" @click="openOrderTrace(c)">查看历史派单</el-button>
        </div>
      </div>
    </div>
    <div v-else-if="requestId" class="panel empty-note">暂无候选组合，请确认机具可作业类型与服务价目是否已维护</div>

    <div class="panel">
      <div class="panel-title">派单列表<span class="hint">默认按更新时间倒序，待作业的单可取消重排</span></div>
      <el-table :data="orders" v-loading="loadingOrders" stripe size="small">
        <el-table-column prop="orderNo" label="派单号" min-width="140" />
        <el-table-column prop="requestNo" label="需求编号" min-width="140" />
        <el-table-column prop="machineNo" label="机具" min-width="100" />
        <el-table-column prop="operatorName" label="农机手" min-width="90" />
        <el-table-column label="计划时段" min-width="250">
          <template #default="{ row }">{{ formatTime(row.planStart) }} ~ {{ formatTime(row.planEnd) }}</template>
        </el-table-column>
        <el-table-column label="面积(亩)" min-width="86" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.planArea) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待作业'" link type="danger" @click="cancelOrder(row)">取消派单</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="orderTotal"
                       v-model:current-page="orderQuery.page" :page-size="8" @change="loadOrders" />
      </div>
    </div>

    <el-dialog v-model="dispatchVisible" title="确认派单" width="560">
      <el-form :model="dispatchForm" label-width="96px">
        <el-form-item label="需求"><b>{{ current?.requestNo }}</b> · {{ current?.serviceType }} {{ current?.area }} 亩</el-form-item>
        <el-form-item label="机具">{{ picked?.machineNo }} · {{ picked?.machineName }}</el-form-item>
        <el-form-item label="农机手">{{ picked?.operatorName }}（准时率 {{ picked?.ontimeRate }}%，操作证至 {{ formatDate(picked?.certExpire) }}）</el-form-item>
        <el-form-item label="计划开始" required>
          <el-date-picker v-model="dispatchForm.planStart" type="datetime" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="计划结束" required>
          <el-date-picker v-model="dispatchForm.planEnd" type="datetime" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="派单说明">
          <el-input v-model="dispatchForm.remark" type="textarea" :rows="2" placeholder="如：按推荐组合派单，机手与农户已有合作" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon
                  title="派单时后端会再次校验：机具状态、机具能力、操作证有效期、机具与机手时段冲突" />
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="dispatching" @click="doDispatch">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cpu } from '@element-plus/icons-vue'
import { dispatchApi, requestApi } from '../api'
import { formatDate, formatTime, levelType, num, statusType } from '../utils/format'

const queue = ref([])
const requestId = ref(null)
const current = ref(null)
const candidates = ref([])
const rule = ref(null)
const evaluating = ref(false)

const orders = ref([])
const orderTotal = ref(0)
const loadingOrders = ref(false)
const orderQuery = reactive({ page: 1, size: 8 })

const dispatchVisible = ref(false)
const picked = ref(null)
const dispatching = ref(false)
const dispatchForm = reactive({ planStart: '', planEnd: '', remark: '' })

function barColor(v) {
  if (v >= 90) return '#2b7955'
  if (v >= 70) return '#356a7a'
  if (v >= 50) return '#b98900'
  return '#b02a37'
}

async function loadQueue() {
  queue.value = await requestApi.schedulable()
  if (!requestId.value && queue.value.length) {
    requestId.value = queue.value[0].id
    await loadCandidates()
  }
}

async function loadCandidates() {
  if (!requestId.value) return
  const res = await dispatchApi.candidates(requestId.value)
  current.value = res.request
  rule.value = res.rule
  candidates.value = res.candidates || []
}

async function evaluate() {
  evaluating.value = true
  try {
    const res = await dispatchApi.evaluate({ requestId: requestId.value })
    current.value = res.request
    rule.value = res.rule
    candidates.value = res.candidates || []
    ElMessage.success('已保存计算留痕，可在「派单规则配置」中查看')
  } finally {
    evaluating.value = false
  }
}

function openDispatch(c) {
  picked.value = c
  const d = new Date((current.value.hopeDate || '').replace(/-/g, '/') + ' 08:00:00')
  const pad = (n) => String(n).padStart(2, '0')
  const fmt = (x) => `${x.getFullYear()}-${pad(x.getMonth() + 1)}-${pad(x.getDate())} ${pad(x.getHours())}:${pad(x.getMinutes())}:00`
  const start = isNaN(d.getTime()) ? new Date() : d
  const end = new Date(start.getTime() + Math.max(4, Math.ceil(current.value.area / 4) + 3) * 3600 * 1000)
  dispatchForm.planStart = fmt(start)
  dispatchForm.planEnd = fmt(end)
  dispatchForm.remark = '依据联合推荐组合派单'
  dispatchVisible.value = true
}

async function doDispatch() {
  dispatching.value = true
  try {
    await dispatchApi.dispatch({
      requestId: requestId.value,
      machineId: picked.value.machineId,
      operatorId: picked.value.operatorId,
      scoreId: picked.value.scoreId || null,
      planStart: dispatchForm.planStart,
      planEnd: dispatchForm.planEnd,
      remark: dispatchForm.remark
    })
    ElMessage.success('派单成功，需求进入待作业')
    dispatchVisible.value = false
    await loadQueue()
    await loadOrders()
  } finally {
    dispatching.value = false
  }
}

function openOrderTrace() {
  ElMessage.info('可在下方派单列表按机具筛选查看历史派单')
}

async function loadOrders() {
  loadingOrders.value = true
  try {
    const res = await dispatchApi.orderPage({ page: orderQuery.page, size: orderQuery.size })
    orders.value = res.records
    orderTotal.value = res.total
  } finally {
    loadingOrders.value = false
  }
}

async function cancelOrder(row) {
  const { value } = await ElMessageBox.prompt('请填写取消原因（不少于 5 个字）', '取消派单', {
    inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字'
  })
  await dispatchApi.cancelOrder(row.id, value)
  ElMessage.success('已取消，需求回到待排程')
  await loadOrders()
  await loadQueue()
}

onMounted(async () => {
  await loadQueue()
  await loadOrders()
})
</script>

<style scoped>
.rule-chip { font-size: 12px; color: #5a6b7a; background: #eef3f6; border: 1px solid #dfe8ed; border-radius: 6px; padding: 6px 10px; }
.tip { font-size: 12px; color: #93a3b0; }
.req-strip {
  display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid #e6ebef;
  border-radius: 8px; padding: 10px 16px; margin-bottom: 12px; font-size: 13px; color: #35495c;
}
.req-strip b { color: #19324a; }
.cand-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.cand-card { background: #fff; border: 1px solid #e6ebef; border-radius: 8px; padding: 14px; display: flex; flex-direction: column; }
.cand-card.best { border-color: #2b7955; box-shadow: 0 0 0 1px rgba(43,121,85,.16); }
.cand-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.cand-title { font-size: 14px; font-weight: 600; color: #19324a; display: flex; align-items: center; gap: 6px; }
.cand-sub { font-size: 12px; color: #7d8e9c; margin-top: 3px; }
.cand-score { text-align: right; }
.score-value { font-size: 24px; font-weight: 700; color: #19324a; font-variant-numeric: tabular-nums; line-height: 1; }
.factors { display: flex; flex-direction: column; gap: 7px; margin-bottom: 10px; }
.factor-label { display: flex; justify-content: space-between; font-size: 12px; color: #45596b; }
.factor-label em { font-style: normal; color: #9aa9b5; margin-left: 6px; }
.factor-label b { font-variant-numeric: tabular-nums; }
.bar { height: 5px; background: #eef2f5; border-radius: 3px; overflow: hidden; margin-top: 3px; }
.bar i { display: block; height: 100%; border-radius: 3px; }
.explain { font-size: 12px; color: #5a6b7a; line-height: 1.7; border-top: 1px dashed #eef2f5; padding-top: 8px; flex: 1; }
.explain p { margin: 0 0 4px; }
.explain b { color: #35495c; }
.warn { color: #b02a37; }
.cand-actions { display: flex; gap: 8px; margin-top: 10px; }
.muted { color: #b6c1ca; }
@media (max-width: 1200px) { .cand-grid { grid-template-columns: 1fr; } }
</style>
