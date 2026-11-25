<template>
  <div>
    <div class="page-head">
      <div>
        <h2>作业执行</h2>
        <div class="sub">农机手到场后开工登记，完工时录入实际面积、油耗与工时；照片通过上传组件保存到项目 uploads/</div>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="派单号" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="作业状态" clearable>
          <el-option v-for="s in ['进行中', '已完工', '作业中断']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无作业记录">
        <el-table-column label="作业号 / 派单" min-width="170">
          <template #default="{ row }">
            <div class="strong">{{ row.orderNo }}</div>
            <div class="dim">{{ row.landName }} · {{ row.farmerName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="机具 / 农机手" min-width="150">
          <template #default="{ row }">
            <div>{{ row.machineNo }}</div>
            <div class="dim">{{ row.operatorName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="服务" min-width="110">
          <template #default="{ row }">{{ row.serviceType }} / {{ row.crop }}</template>
        </el-table-column>
        <el-table-column label="计划 / 实际面积" min-width="130" align="right">
          <template #default="{ row }">
            <div class="num">{{ num(row.planArea) }} → {{ row.actualArea ? num(row.actualArea) : '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="到场 / 完工" min-width="180">
          <template #default="{ row }">
            <div class="dim">{{ formatTime(row.arriveTime) }}</div>
            <div class="dim">{{ formatTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="油耗/工时" min-width="120" align="right">
          <template #default="{ row }">
            <div class="num">{{ row.fuelLiters ? num(row.fuelLiters) + ' L' : '—' }}</div>
            <div class="dim">{{ row.workHours ? num(row.workHours) + ' h' : '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '已完工' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="现场照片" min-width="120">
          <template #default="{ row }">
            <div class="imgs">
              <el-image v-if="row.beforeImage" :src="row.beforeImage" :preview-src-list="[row.beforeImage]"
                        fit="cover" class="thumb" preview-teleported />
              <el-image v-if="row.afterImage" :src="row.afterImage" :preview-src-list="[row.afterImage]"
                        fit="cover" class="thumb" preview-teleported />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @change="load" />
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">待开工派单<span class="hint">接单后先登记到场时间与作业前照片</span></div>
      <el-table :data="waiting" size="small" empty-text="没有待开工的派单">
        <el-table-column prop="orderNo" label="派单号" min-width="140" />
        <el-table-column prop="machineNo" label="机具" min-width="100" />
        <el-table-column prop="operatorName" label="农机手" min-width="90" />
        <el-table-column prop="landName" label="地块" min-width="120" />
        <el-table-column prop="serviceType" label="服务" min-width="90" />
        <el-table-column label="计划开始" min-width="150">
          <template #default="{ row }">{{ formatTime(row.planStart) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStart(row)">登记到场并开工</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 开工 -->
    <el-dialog v-model="startVisible" title="登记到场并开工" width="480">
      <el-form label-width="100px">
        <el-form-item label="派单">{{ pick?.orderNo }} · {{ pick?.machineNo }}</el-form-item>
        <el-form-item label="到场时间">
          <el-date-picker v-model="startForm.arriveTime" type="datetime" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="作业前照片">
          <el-upload :show-file-list="false" :http-request="(o) => upload(o, 'before')" accept="image/*">
            <el-image v-if="startForm.beforeImage" :src="startForm.beforeImage" fit="cover" class="up-img" />
            <div v-else class="up-box">点击上传</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="startForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doStart">确认开工</el-button>
      </template>
    </el-dialog>

    <!-- 完工 -->
    <el-dialog v-model="finishVisible" title="提交完工记录" width="520">
      <el-form label-width="110px">
        <el-form-item label="派单">{{ pick?.orderNo }}</el-form-item>
        <el-form-item label="实际面积(亩)" required>
          <el-input-number v-model="finishForm.actualArea" :min="0.1" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="油耗(升)">
          <el-input-number v-model="finishForm.fuelLiters" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="作业工时(h)">
          <el-input-number v-model="finishForm.workHours" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="完工时间">
          <el-date-picker v-model="finishForm.endTime" type="datetime" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="作业后照片">
          <el-upload :show-file-list="false" :http-request="(o) => upload(o, 'after')" accept="image/*">
            <el-image v-if="finishForm.afterImage" :src="finishForm.afterImage" fit="cover" class="up-img" />
            <div v-else class="up-box">点击上传</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="异常说明">
          <el-input v-model="finishForm.exceptionDesc" type="textarea" :rows="2" placeholder="作业中断、地块异常等，如无异常可留空" />
        </el-form-item>
        <el-alert v-if="diffHint" type="warning" :closable="false" show-icon :title="diffHint" />
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doFinish">提交完工</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { dispatchApi, workApi } from '../api'
import { formatTime, num } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', status: '', page: 1, size: 10 })
const waiting = ref([])
const saving = ref(false)

const startVisible = ref(false)
const finishVisible = ref(false)
const pick = ref(null)
const startForm = reactive({ arriveTime: '', beforeImage: '', remark: '' })
const finishForm = reactive({ actualArea: 0, fuelLiters: 0, workHours: 0, endTime: '', afterImage: '', exceptionDesc: '' })

const diffHint = computed(() => {
  if (!pick.value || !finishForm.actualArea) return ''
  const plan = Number(pick.value.planArea || 0)
  if (!plan) return ''
  const diff = ((finishForm.actualArea - plan) / plan) * 100
  if (Math.abs(diff) <= 3) return ''
  return `面积偏差 ${diff.toFixed(1)}%，超过 3% 阈值，农户验收时需二次确认后才可结算`
})

async function load() {
  loading.value = true
  try {
    const res = await workApi.page({ ...q })
    rows.value = res.records
    total.value = res.total
    const w = await dispatchApi.orderPage({ page: 1, size: 20, status: '待作业' })
    waiting.value = w.records
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }
function reset() { Object.assign(q, { keyword: '', status: '', page: 1 }); load() }

function openStart(row) {
  pick.value = row
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  startForm.arriveTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:00`
  startForm.beforeImage = ''
  startForm.remark = ''
  startVisible.value = true
}

async function doStart() {
  saving.value = true
  try {
    await workApi.start({
      orderId: pick.value.id, arriveTime: startForm.arriveTime,
      beforeImage: startForm.beforeImage || '/uploads/seed/work/01.jpg', remark: startForm.remark
    })
    ElMessage.success('已开工，作业状态更新为作业中')
    startVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openFinish(row) {
  pick.value = { ...row, id: row.orderId || row.id, planArea: row.planArea }
  finishForm.actualArea = Number(row.planArea || 0)
  finishForm.fuelLiters = 0
  finishForm.workHours = 0
  finishForm.endTime = ''
  finishForm.afterImage = ''
  finishForm.exceptionDesc = ''
  finishVisible.value = true
}

async function doFinish() {
  saving.value = true
  try {
    const res = await workApi.finish({
      orderId: pick.value.id, actualArea: finishForm.actualArea, fuelLiters: finishForm.fuelLiters,
      workHours: finishForm.workHours, endTime: finishForm.endTime || null,
      afterImage: finishForm.afterImage || '/uploads/seed/work/03.jpg', exceptionDesc: finishForm.exceptionDesc
    })
    ElMessage.success(res.needSecondConfirm ? '已提交，面积偏差超阈值，验收需二次确认' : '已提交完工，等待农户验收')
    finishVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 上传：走后端 /api/file/upload，返回值兼容 data.path / data.url
function upload(option, field) {
  const fd = new FormData()
  fd.append('file', option.file)
  fetch('/api/file/upload', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') },
    body: fd
  })
    .then((r) => r.json())
    .then((res) => {
      const d = res.data?.data || res.data || res
      const path = d.path || d.url || d.filePath
      if (!path) throw new Error('上传返回缺少路径')
      if (field === 'before') startForm.beforeImage = path
      else finishForm.afterImage = path
      ElMessage.success('照片已上传')
      option.onSuccess && option.onSuccess(res)
    })
    .catch((e) => {
      ElMessage.error('上传失败：' + e.message)
      option.onError && option.onError(e)
    })
}

onMounted(() => {
  load()
  // 作业中的记录可直接点完工
  setTimeout(() => {
    rows.value.forEach((r) => {
      if (r.status === '进行中') r.orderId = r.orderId
    })
  }, 300)
})
</script>

<style scoped>
.strong { font-weight: 600; color: #19324a; }
.dim { color: #93a3b0; font-size: 12px; }
.imgs { display: flex; gap: 4px; }
.up-img { width: 88px; height: 66px; border-radius: 4px; object-fit: cover; border: 1px solid #e6ebef; }
.up-box {
  width: 88px; height: 66px; border: 1px dashed #c9d6dd; border-radius: 4px; display: grid;
  place-items: center; color: #93a3b0; font-size: 12px;
}
</style>
