<template>
  <div>
    <div class="page-head">
      <div>
        <h2>服务需求池</h2>
        <div class="sub">按状态与作业类型筛选；需求提交后进入「待报价」，由调度员生成报价</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openForm()">新建需求</el-button>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="需求编号 / 农户 / 地块" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="需求状态" clearable>
          <el-option v-for="s in REQUEST_STATUS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="q.serviceType" placeholder="服务类型" clearable>
          <el-option v-for="s in SERVICE_TYPES" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="q.crop" placeholder="作物" clearable>
          <el-option v-for="s in CROPS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="q.urgency" placeholder="紧急度" clearable>
          <el-option v-for="s in URGENCY" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="当前筛选条件下无记录，清空筛选或新建需求">
        <el-table-column prop="requestNo" label="需求编号" min-width="150" />
        <el-table-column prop="farmerName" label="农户" min-width="90" />
        <el-table-column prop="landName" label="地块" min-width="130" />
        <el-table-column prop="serviceType" label="服务类型" min-width="90" />
        <el-table-column prop="crop" label="作物" min-width="80" />
        <el-table-column label="面积(亩)" min-width="90" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.area) }}</span></template>
        </el-table-column>
        <el-table-column label="希望日期" min-width="110">
          <template #default="{ row }">{{ formatDate(row.hopeDate) }}</template>
        </el-table-column>
        <el-table-column label="紧急度" min-width="86">
          <template #default="{ row }">
            <el-tag size="small" :type="row.urgency === '普通' ? 'info' : (row.urgency === '加急' ? 'warning' : 'danger')">
              {{ row.urgency }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额(元)" min-width="110" align="right">
          <template #default="{ row }">{{ row.totalAmount > 0 ? money(row.totalAmount) : '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === '待确认'" link type="primary" @click="confirm(row)">确认报价</el-button>
            <el-button v-if="['待报价', '已退回'].includes(row.status)" link type="primary" @click="openForm(row)">修改</el-button>
            <el-button v-if="['待报价', '已退回'].includes(row.status)" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>

    <!-- 新建 / 修改需求 -->
    <el-drawer v-model="formVisible" :title="form.id ? '修改需求' : '新建作业需求'" size="480px" :before-close="beforeClose">
      <el-form :model="form" label-width="96px" label-position="right">
        <el-form-item label="地块" required>
          <el-select v-model="form.landId" placeholder="选择地块" style="width: 100%" @change="onLand">
            <el-option v-for="l in lands" :key="l.id" :label="`${l.landName}（${l.area} 亩 / ${l.crop || '—'}）`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务类型" required>
          <el-select v-model="form.serviceType" placeholder="选择服务类型" style="width: 100%">
            <el-option v-for="s in SERVICE_TYPES" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="作物">
          <el-select v-model="form.crop" placeholder="作物" style="width: 100%">
            <el-option v-for="s in CROPS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业面积" required>
          <el-input-number v-model="form.area" :min="0.1" :precision="1" :step="0.5" style="width: 100%" />
          <div class="hint">不得大于地块面积 {{ selectedLand?.area || '—' }} 亩</div>
        </el-form-item>
        <el-form-item label="希望日期" required>
          <el-date-picker v-model="form.hopeDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="紧急度">
          <el-radio-group v-model="form.urgency">
            <el-radio-button v-for="u in URGENCY" :key="u" :value="u">{{ u }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="特殊要求">
          <el-input v-model="form.requireDesc" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="如：按垄作业、地头有电线杆需注意" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交</el-button>
      </template>
    </el-drawer>

    <!-- 需求详情 -->
    <el-drawer v-model="detailVisible" title="需求详情" size="620px">
      <template v-if="detail">
        <div class="detail-head">
          <div>
            <strong>{{ detail.request.requestNo }}</strong>
            <el-tag size="small" :type="statusType(detail.request.status)" style="margin-left: 8px">
              {{ detail.request.status }}
            </el-tag>
          </div>
          <div class="who">
            农户 {{ detail.request.farmerName }} · 地块 {{ detail.request.landName }} ·
            提交 {{ formatTime(detail.request.createTime) }}
          </div>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="服务类型">{{ detail.request.serviceType }}</el-descriptions-item>
          <el-descriptions-item label="作物">{{ detail.request.crop }}</el-descriptions-item>
          <el-descriptions-item label="面积">{{ detail.request.area }} 亩</el-descriptions-item>
          <el-descriptions-item label="希望日期">{{ formatDate(detail.request.hopeDate) }}</el-descriptions-item>
          <el-descriptions-item label="紧急度">{{ detail.request.urgency }}</el-descriptions-item>
          <el-descriptions-item label="确认金额">{{ detail.request.totalAmount > 0 ? money(detail.request.totalAmount) : '—' }}</el-descriptions-item>
          <el-descriptions-item label="特殊要求" :span="2">{{ detail.request.requireDesc || '无' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.request.backReason" label="退回原因" :span="2">
            <span style="color: #b02a37">{{ detail.request.backReason }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="panel-title" style="margin-top: 18px">报价记录</div>
        <el-table :data="detail.quotations" size="small" empty-text="尚未生成报价">
          <el-table-column prop="quotationNo" label="报价单号" min-width="140" />
          <el-table-column label="合计(元)" min-width="100" align="right">
            <template #default="{ row }">{{ money(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column prop="quoteStatus" label="状态" min-width="86" />
          <el-table-column label="生成时间" min-width="140">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>

        <div class="panel-title" style="margin-top: 18px">派单记录</div>
        <el-table :data="detail.orders" size="small" empty-text="尚未派单">
          <el-table-column prop="orderNo" label="派单号" min-width="140" />
          <el-table-column prop="machineNo" label="机具" min-width="100" />
          <el-table-column prop="operatorName" label="农机手" min-width="90" />
          <el-table-column prop="dispatchStatus" label="状态" min-width="86" />
        </el-table>

        <div class="panel-title" style="margin-top: 18px">操作时间轴</div>
        <el-timeline>
          <el-timeline-item v-for="r in detail.records" :key="r.id" :timestamp="formatTime(r.createTime)" placement="top">
            <b>{{ r.action }}</b>
            <span v-if="r.fromStatus" class="tl-status">{{ r.fromStatus }} → {{ r.toStatus }}</span>
            <div v-if="r.reason" class="tl-reason">{{ r.reason }}</div>
            <div class="tl-who">{{ r.operatorName }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { machineApi, requestApi } from '../api'
import { CROPS, REQUEST_STATUS, SERVICE_TYPES, URGENCY, formatDate, formatTime, money, num, statusType } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const lands = ref([])
const q = reactive({ keyword: '', status: '', serviceType: '', crop: '', urgency: '', page: 1, size: 10 })

const formVisible = ref(false)
const form = reactive({ id: null, landId: null, serviceType: '', crop: '', area: 1, hopeDate: '', urgency: '普通', requireDesc: '' })
const selectedLand = computed(() => lands.value.find((l) => l.id === form.landId))

const detailVisible = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await requestApi.page({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', status: '', serviceType: '', crop: '', urgency: '', page: 1 })
  load()
}

async function loadLands() {
  const res = await machineApi.landPage({ page: 1, size: 100 })
  lands.value = res.records
}

function openForm(row) {
  Object.assign(form, {
    id: row?.id ?? null, landId: row?.landId ?? null, serviceType: row?.serviceType ?? '',
    crop: row?.crop ?? '', area: row?.area ?? 1, hopeDate: row?.hopeDate ?? '',
    urgency: row?.urgency ?? '普通', requireDesc: row?.requireDesc ?? ''
  })
  formVisible.value = true
}

function onLand(id) {
  const l = lands.value.find((x) => x.id === id)
  if (l) {
    form.crop = l.crop
    form.area = l.area
  }
}

async function save() {
  if (!form.landId) return ElMessage.warning('请选择地块')
  if (!form.serviceType) return ElMessage.warning('请选择服务类型')
  if (!form.hopeDate) return ElMessage.warning('请选择希望作业日期')
  if (selectedLand.value && form.area > selectedLand.value.area) {
    return ElMessage.warning('作业面积不能大于地块面积')
  }
  saving.value = true
  try {
    await requestApi.save({ ...form })
    ElMessage.success('已提交')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function beforeClose(done) {
  done()
}

async function openDetail(row) {
  detail.value = await requestApi.detail(row.id)
  detailVisible.value = true
}

async function confirm(row) {
  await ElMessageBox.confirm(`确认接受报价 ${money(row.totalAmount)}？确认后进入排程。`, '确认报价', { type: 'warning' })
  await requestApi.confirmQuote(row.id)
  ElMessage.success('已确认，进入待排程')
  load()
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('请填写取消原因（不少于 5 个字）', '取消需求', {
    inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字'
  })
  await requestApi.cancel(row.id, value)
  ElMessage.success('已取消')
  load()
}

onMounted(async () => {
  await loadLands()
  await load()
})
</script>

<style scoped>
.hint { font-size: 11px; color: #93a3b0; margin-top: 2px; }
.detail-head { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
.who { color: #6b7c8c; font-size: 12px; }
.tl-status { color: #356a7a; font-size: 12px; margin-left: 8px; }
.tl-reason { color: #5a6b7a; font-size: 12px; margin-top: 2px; }
.tl-who { color: #9aa9b5; font-size: 11px; margin-top: 2px; }
</style>
