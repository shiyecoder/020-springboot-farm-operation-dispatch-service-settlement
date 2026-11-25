<template>
  <div>
    <div class="page-head">
      <div>
        <h2>机具档案</h2>
        <div class="sub">维护机具类型、功率、作业宽度、年检与保养周期；保养中/维修中/停用的机具不参与派单</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增机具</el-button>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="编号 / 名称 / 型号" clearable @keyup.enter="search" />
        <el-select v-model="q.machineType" placeholder="机具类型" clearable>
          <el-option v-for="t in MACHINE_TYPES" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="q.status" placeholder="可用状态" clearable>
          <el-option v-for="s in MACHINE_STATUS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="q.region" placeholder="区域" clearable style="width: 150px" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="没有符合条件的机具">
        <el-table-column label="照片" width="76">
          <template #default="{ row }">
            <el-image :src="row.coverImage" :preview-src-list="[row.coverImage]" fit="cover" class="thumb" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="machineNo" label="机具编号" min-width="100" />
        <el-table-column prop="machineName" label="名称" min-width="180" />
        <el-table-column prop="machineType" label="类型" min-width="100" />
        <el-table-column prop="model" label="型号" min-width="100" />
        <el-table-column label="功率/幅宽" min-width="110" align="right">
          <template #default="{ row }">{{ row.powerHp || '—' }} 马力 / {{ row.workWidth }} m</template>
        </el-table-column>
        <el-table-column prop="workTypes" label="可作业类型" min-width="140" />
        <el-table-column label="累计工时" min-width="96" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.totalHours) }}</span></template>
        </el-table-column>
        <el-table-column label="年检" min-width="110">
          <template #default="{ row }">
            <span :class="{ warn: row.checkWarn }">{{ formatDate(row.annualCheckDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="区域" min-width="96" />
        <el-table-column label="状态" min-width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="['空闲'].includes(row.status) ? 'success' : (['作业中'].includes(row.status) ? 'primary' : 'warning')">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="primary" @click="openStatus(row)">状态变更</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>

    <el-drawer v-model="dialog" :title="form.id ? '编辑机具' : '新增机具'" size="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="机具编号" required><el-input v-model="form.machineNo" maxlength="50" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.machineName" /></el-form-item>
        <el-form-item label="机具类型" required>
          <el-select v-model="form.machineType" style="width: 100%">
            <el-option v-for="t in MACHINE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号" required><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="功率(马力)"><el-input-number v-model="form.powerHp" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="作业宽度(m)"><el-input-number v-model="form.workWidth" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="可作业类型">
          <el-select v-model="workTypes" multiple style="width: 100%">
            <el-option v-for="t in SERVICE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="累计工时" required><el-input-number v-model="form.totalHours" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="年检日期">
          <el-date-picker v-model="form.annualCheckDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次保养">
          <el-date-picker v-model="form.nextMaintainDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当前区域"><el-input v-model="form.region" maxlength="100" /></el-form-item>
        <el-form-item label="台班成本"><el-input-number v-model="form.hourlyCost" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="亩均油耗">
          <el-input-number v-model="form.fuelPerMu" :min="0" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="机具照片">
          <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
            <el-image v-if="form.coverImage" :src="form.coverImage" fit="cover" class="up-img" />
            <div v-else class="up-box">点击上传</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="statusDialog" title="机具状态变更" width="420">
      <el-form label-width="90px">
        <el-form-item label="机具">{{ statusRow?.machineNo }} · {{ statusRow?.machineName }}</el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option v-for="s in MACHINE_STATUS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="statusForm.reason" type="textarea" :rows="2" placeholder="如：更换液压油，预计 2 天（不少于 5 个字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStatus">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { machineApi } from '../api'
import { MACHINE_STATUS, MACHINE_TYPES, SERVICE_TYPES, formatDate, num } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', machineType: '', status: '', region: '', page: 1, size: 10 })
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})

const workTypes = computed({
  get: () => (form.workTypes ? form.workTypes.split(',').filter(Boolean) : []),
  set: (v) => { form.workTypes = v.join(',') }
})

const statusDialog = ref(false)
const statusRow = ref(null)
const statusForm = reactive({ status: '空闲', reason: '' })

async function load() {
  loading.value = true
  try {
    const res = await machineApi.page({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row, status: undefined } : {
    machineNo: '', machineName: '', machineType: '拖拉机', model: '', powerHp: 0,
    workWidth: 0, workTypes: '', totalHours: 0, region: '城郊片区', hourlyCost: 0, fuelPerMu: 2
  })
  dialog.value = true
}

async function save() {
  if (!form.machineNo || !form.model) return ElMessage.warning('机具编号与型号必填')
  saving.value = true
  try {
    await machineApi.save({ ...form, currentStatus: form.currentStatus || '空闲' })
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openStatus(row) {
  statusRow.value = row
  statusForm.status = row.status
  statusForm.reason = ''
  statusDialog.value = true
}

async function saveStatus() {
  if (statusForm.status !== '空闲' && statusForm.reason.trim().length < 5) {
    return ElMessage.warning('原因不少于 5 个字')
  }
  await machineApi.changeStatus(statusRow.value.id, { status: statusForm.status, reason: statusForm.reason })
  ElMessage.success('状态已更新')
  statusDialog.value = false
  load()
}

function upload(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fetch('/api/file/upload', { method: 'POST', headers: { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }, body: fd })
    .then((r) => r.json())
    .then((res) => {
      const d = res.data?.data || res.data || res
      form.coverImage = d.path || d.url || d.filePath
      ElMessage.success('已上传')
    })
    .catch(() => ElMessage.error('上传失败'))
}

onMounted(load)
</script>

<style scoped>
.warn { color: #b02a37; }
.up-img { width: 100px; height: 74px; border-radius: 4px; object-fit: cover; border: 1px solid #e6ebef; }
.up-box { width: 100px; height: 74px; border: 1px dashed #c9d6dd; border-radius: 4px; display: grid; place-items: center; color: #93a3b0; font-size: 12px; }
</style>
