<template>
  <div>
    <div class="page-head">
      <div>
        <h2>派单规则配置</h2>
        <div class="sub">创新点「机具能力与成本联合派单」：权重合计必须 100%，保存即生成新版本并停用旧版本</div>
      </div>
      <el-button type="primary" :loading="saving" @click="save">保存为新版本</el-button>
    </div>

    <div class="layout">
      <div class="panel">
        <div class="panel-title">权重与阈值<span class="hint">当前生效 {{ active?.ruleVersion }}，合计 {{ weightSum }}%</span></div>
        <div v-for="f in factors" :key="f.key" class="row">
          <div class="row-label">{{ f.name }}</div>
          <el-slider v-model="weights[f.key]" :min="0" :max="60" :step="5" show-stops style="flex: 1" />
          <el-input-number v-model="weights[f.key]" :min="0" :max="100" :step="5" size="small" style="width: 96px" />
          <span class="unit">%</span>
        </div>
        <div class="sum" :class="{ bad: weightSum !== 100 }">合计 {{ weightSum }}%（必须等于 100%）</div>
        <el-form label-width="110px" style="margin-top: 12px">
          <el-form-item label="推荐阈值">
            <el-input-number v-model="levelHigh" :min="50" :max="100" style="width: 120px" />
            <span class="hint">总分 ≥ 该值为「推荐」</span>
          </el-form-item>
          <el-form-item label="可选阈值">
            <el-input-number v-model="levelMid" :min="0" :max="99" style="width: 120px" />
            <span class="hint">否则为「不推荐」</span>
          </el-form-item>
          <el-form-item label="变更说明">
            <el-input v-model="remark" type="textarea" :rows="2" maxlength="120" placeholder="如：按春季作业复盘上调距离权重" />
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" show-icon
                  title="修改权重只影响新计算或主动重新计算的数据，历史留痕保留原规则版本" />
      </div>

      <div class="panel">
        <div class="panel-title">规则版本</div>
        <el-timeline>
          <el-timeline-item v-for="r in rules" :key="r.id" :timestamp="formatTime(r.effectiveTime || r.createTime)" placement="top">
            <div class="ver">
              <b>{{ r.ruleVersion }}</b>
              <el-tag size="small" :type="r.ruleStatus === '启用' ? 'success' : 'info'">{{ r.ruleStatus }}</el-tag>
            </div>
            <div class="ver-weights">
              能力 {{ r.wCapability }} / 资质 {{ r.wCert }} / 空闲 {{ r.wFree }} /
              距离 {{ r.wDistance }} / 油耗 {{ r.wFuel }} / 准时率 {{ r.wOntime }}
            </div>
            <div class="ver-remark">{{ r.remark || '—' }}</div>
          </el-timeline-item>
        </el-timeline>
        <div class="effect" v-if="effect">
          <div class="effect-item"><span>累计计算</span><b>{{ effect.total }}</b></div>
          <div class="effect-item"><span>推荐占比</span><b>{{ effect.adoptRate }}%</b></div>
          <div class="effect-item"><span>人工调整率</span><b>{{ effect.adjustRate }}%</b></div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">
        计算留痕
        <el-select v-model="level" size="small" placeholder="结果等级" clearable style="width: 140px" @change="loadScores">
          <el-option v-for="l in ['推荐', '可选', '不推荐']" :key="l" :label="l" :value="l" />
        </el-select>
      </div>
      <el-table :data="scores" v-loading="loading" size="small" stripe empty-text="暂无计算记录，到排程台执行一次正式计算">
        <el-table-column prop="requestNo" label="需求" min-width="140" />
        <el-table-column prop="machineNo" label="机具" min-width="90" />
        <el-table-column prop="operatorName" label="农机手" min-width="86" />
        <el-table-column label="六项得分" min-width="250">
          <template #default="{ row }">
            <span class="score-line">
              能力 {{ row.capabilityScore }} · 资质 {{ row.certScore }} · 空闲 {{ row.freeScore }} ·
              距离 {{ row.distanceScore }} · 油耗 {{ row.fuelScore }} · 准时 {{ row.ontimeScore }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="总分" min-width="80" align="right">
          <template #default="{ row }"><b class="num">{{ row.totalScore }}</b></template>
        </el-table-column>
        <el-table-column label="结果" min-width="150">
          <template #default="{ row }">
            <el-tag size="small" :type="levelType(row.resultLevel)">{{ row.resultLevel }}</el-tag>
            <el-tag v-if="row.adjusted" size="small" type="warning" style="margin-left: 4px">
              人工→{{ row.finalLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则" min-width="70"><template #default="{ row }">{{ row.ruleVersion }}</template></el-table-column>
        <el-table-column label="操作" min-width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">依据</el-button>
            <el-button link type="warning" @click="openAdjust(row)">人工调整</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="scoreTotal"
                       v-model:current-page="page" :page-size="10" @change="loadScores" />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="计算依据" size="520px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="需求">{{ detail.score.requestNo }}</el-descriptions-item>
          <el-descriptions-item label="规则版本">{{ detail.score.ruleVersion }}</el-descriptions-item>
          <el-descriptions-item label="机具">{{ detail.score.machineNo }}</el-descriptions-item>
          <el-descriptions-item label="农机手">{{ detail.score.operatorName }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ detail.score.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detail.score.resultLevel }}</el-descriptions-item>
        </el-descriptions>
        <div class="panel-title" style="margin-top: 16px">结果解释</div>
        <p class="explain">{{ detail.score.explainText }}</p>
        <div class="panel-title">未满足条件</div>
        <p class="explain" :class="{ warn: detail.score.unmetText !== '无' }">{{ detail.score.unmetText }}</p>
        <div class="panel-title">可改进项</div>
        <p class="explain">{{ detail.score.improveText }}</p>
        <div class="panel-title">输入快照</div>
        <pre class="snap">{{ pretty(detail.score.snapshot) }}</pre>
        <div v-if="detail.score.adjustReason" class="panel-title">人工调整记录</div>
        <p v-if="detail.score.adjustReason" class="explain">
          {{ detail.score.adjustBy }} 于 {{ formatTime(detail.score.adjustTime) }} 调整为
          {{ detail.score.finalLevel }}：{{ detail.score.adjustReason }}
        </p>
      </template>
    </el-drawer>

    <el-dialog v-model="adjustVisible" title="人工调整最终结果" width="440">
      <el-form label-width="96px">
        <el-form-item label="原始结果">{{ pick?.resultLevel }}（{{ pick?.totalScore }} 分，不可覆盖）</el-form-item>
        <el-form-item label="调整后">
          <el-radio-group v-model="adjustForm.finalLevel">
            <el-radio-button v-for="l in ['推荐', '可选', '不推荐']" :key="l" :value="l">{{ l }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" placeholder="如：机手与该农户长期合作，现场沟通成本更低（不少于 5 个字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="doAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ruleApi } from '../api'
import { formatTime, levelType } from '../utils/format'

const factors = [
  { key: 'capability', name: '能力匹配' }, { key: 'cert', name: '资质有效期' },
  { key: 'free', name: '空闲时段' }, { key: 'distance', name: '到场距离' },
  { key: 'fuel', name: '历史油耗' }, { key: 'ontime', name: '准时率' }
]
const weights = reactive({ capability: 25, cert: 15, free: 20, distance: 15, fuel: 10, ontime: 15 })
const levelHigh = ref(80)
const levelMid = ref(60)
const remark = ref('')
const rules = ref([])
const effect = ref(null)
const saving = ref(false)

const scores = ref([])
const scoreTotal = ref(0)
const page = ref(1)
const level = ref('')
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const adjustVisible = ref(false)
const pick = ref(null)
const adjustForm = reactive({ finalLevel: '推荐', reason: '' })

const weightSum = computed(() => factors.reduce((s, f) => s + Number(weights[f.key] || 0), 0))
const active = computed(() => rules.value.find((r) => r.ruleStatus === '启用'))

async function loadRules() {
  rules.value = await ruleApi.list()
  effect.value = await ruleApi.effect()
}

async function save() {
  if (weightSum.value !== 100) return ElMessage.warning('权重合计必须等于 100%，当前为 ' + weightSum.value + '%')
  if (levelMid.value >= levelHigh.value) return ElMessage.warning('可选阈值必须小于推荐阈值')
  saving.value = true
  try {
    await ruleApi.save({
      ruleName: '机具能力与成本联合派单',
      weights: { ...weights },
      levelHigh: levelHigh.value,
      levelMid: levelMid.value,
      remark: remark.value
    })
    ElMessage.success('已保存为新版本，旧版本已停用')
    remark.value = ''
    loadRules()
  } finally {
    saving.value = false
  }
}

async function loadScores() {
  loading.value = true
  try {
    const res = await ruleApi.scorePage({ page: page.value, size: 10, level: level.value })
    scores.value = res.records
    scoreTotal.value = res.total
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  detail.value = await ruleApi.scoreDetail(row.id)
  detailVisible.value = true
}

function openAdjust(row) {
  pick.value = row
  adjustForm.finalLevel = row.finalLevel
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function doAdjust() {
  if (adjustForm.reason.trim().length < 5) return ElMessage.warning('调整原因不少于 5 个字')
  await ruleApi.adjust(pick.value.id, { finalLevel: adjustForm.finalLevel, reason: adjustForm.reason })
  ElMessage.success('已记录人工调整，原始结果保留')
  adjustVisible.value = false
  loadScores()
}

function pretty(s) {
  try { return JSON.stringify(JSON.parse(s), null, 2) } catch (e) { return s || '—' }
}

onMounted(async () => {
  await loadRules()
  if (active.value) {
    const r = active.value
    weights.capability = Number(r.wCapability)
    weights.cert = Number(r.wCert)
    weights.free = Number(r.wFree)
    weights.distance = Number(r.wDistance)
    weights.fuel = Number(r.wFuel)
    weights.ontime = Number(r.wOntime)
    levelHigh.value = Number(r.levelHigh)
    levelMid.value = Number(r.levelMid)
  }
  loadScores()
})
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 1.35fr 1fr; gap: 14px; }
.row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.row-label { width: 88px; font-size: 13px; color: #35495c; }
.unit { color: #93a3b0; font-size: 12px; }
.sum { font-size: 13px; color: #2b7955; margin-top: 8px; }
.sum.bad { color: #b02a37; }
.hint { font-size: 11px; color: #93a3b0; margin-left: 8px; }
.ver { display: flex; align-items: center; gap: 8px; }
.ver-weights { font-size: 12px; color: #5a6b7a; margin-top: 3px; }
.ver-remark { font-size: 12px; color: #93a3b0; margin-top: 2px; }
.effect { display: flex; gap: 16px; border-top: 1px dashed #eef2f5; padding-top: 10px; margin-top: 6px; }
.effect-item { font-size: 12px; color: #6b7c8c; display: flex; flex-direction: column; gap: 2px; }
.effect-item b { font-size: 16px; color: #19324a; }
.score-line { font-size: 12px; color: #5a6b7a; }
.explain { font-size: 12px; color: #45596b; line-height: 1.8; margin: 0 0 8px; }
.warn { color: #b02a37; }
.snap { background: #f7f9fb; border: 1px solid #e6ebef; border-radius: 6px; padding: 10px; font-size: 11px; color: #45596b; overflow: auto; max-height: 220px; }
@media (max-width: 1100px) { .layout { grid-template-columns: 1fr; } }
</style>
