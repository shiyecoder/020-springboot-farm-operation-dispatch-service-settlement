<template>
  <div>
    <div class="page-head">
      <div>
        <h2>验收管理</h2>
        <div class="sub">农户确认实际面积与作业质量；面积偏差超过 3% 必须二次确认；要求返工时需求回到待排程</div>
      </div>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="派单号" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="验收状态" clearable>
          <el-option label="待验收" value="待验收" />
          <el-option label="已完成" value="已完成" />
          <el-option label="待返工" value="待返工" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="没有待验收的作业">
        <el-table-column label="派单 / 需求" min-width="170">
          <template #default="{ row }">
            <div class="strong">{{ row.orderNo }}</div>
            <div class="dim">{{ row.requestNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="农户 / 地块" min-width="150">
          <template #default="{ row }">
            <div>{{ row.farmerName }}</div>
            <div class="dim">{{ row.landName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="服务" min-width="100">
          <template #default="{ row }">{{ row.serviceType }} / {{ row.crop }}</template>
        </el-table-column>
        <el-table-column label="计划 / 实际面积" min-width="130" align="right">
          <template #default="{ row }">
            <span class="num">{{ num(row.planArea) }} → {{ row.actualArea ? num(row.actualArea) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="面积偏差" min-width="110" align="right">
          <template #default="{ row }">
            <span :class="{ danger: row.needSecondConfirm }">{{ row.areaDiffRate != null ? row.areaDiffRate + '%' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="现场照片" min-width="120">
          <template #default="{ row }">
            <div class="imgs">
              <el-image v-if="row.beforeImage" :src="row.beforeImage" :preview-src-list="[row.beforeImage]" fit="cover" class="thumb" preview-teleported />
              <el-image v-if="row.afterImage" :src="row.afterImage" :preview-src-list="[row.afterImage]" fit="cover" class="thumb" preview-teleported />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="96">
          <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="评分" min-width="70" align="center">
          <template #default="{ row }">{{ row.acceptance?.qualityScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待验收'" link type="primary" @click="openAccept(row)">验收</el-button>
            <span v-else class="dim">已完成</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" title="作业验收" width="540">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 14px">
        <el-descriptions-item label="派单">{{ pick?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="机具">{{ pick?.machineNo }}</el-descriptions-item>
        <el-descriptions-item label="服务">{{ pick?.serviceType }} / {{ pick?.crop }}</el-descriptions-item>
        <el-descriptions-item label="农机手">{{ pick?.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="计划面积">{{ pick?.planArea }} 亩</el-descriptions-item>
        <el-descriptions-item label="实际面积">{{ pick?.actualArea }} 亩</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="110px">
        <el-form-item label="确认面积(亩)" required>
          <el-input-number v-model="form.acceptArea" :min="0.1" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="作业质量">
          <el-rate v-model="form.qualityScore" />
        </el-form-item>
        <el-form-item label="是否返工">
          <el-switch v-model="form.rework" active-text="要求返工" inactive-text="验收通过" />
        </el-form-item>
        <el-form-item v-if="form.rework" label="返工原因" required>
          <el-input v-model="form.reworkReason" type="textarea" :rows="2" placeholder="如：地头约 0.3 亩漏耕，请返工（不少于 5 个字）" />
        </el-form-item>
        <el-form-item v-if="needConfirm && !form.rework" label="面积偏差确认">
          <el-checkbox v-model="form.secondConfirm">
            已知悉面积偏差 {{ pick?.areaDiffRate }}%（超过 3%），确认以 {{ form.acceptArea }} 亩结算
          </el-checkbox>
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="form.comment" type="textarea" :rows="2" maxlength="120" placeholder="作业平整度、机手配合情况等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交验收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { workApi } from '../api'
import { num, statusType } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', status: '', page: 1, size: 10 })
const dialog = ref(false)
const pick = ref(null)
const saving = ref(false)
const form = reactive({ acceptArea: 0, qualityScore: 5, rework: false, reworkReason: '', secondConfirm: false, comment: '' })

const needConfirm = computed(() => !!pick.value?.needSecondConfirm)

async function load() {
  loading.value = true
  try {
    const res = await workApi.acceptPage({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }
function reset() { Object.assign(q, { keyword: '', status: '', page: 1 }); load() }

function openAccept(row) {
  pick.value = row
  form.acceptArea = Number(row.actualArea || row.planArea || 0)
  form.qualityScore = 5
  form.rework = false
  form.reworkReason = ''
  form.secondConfirm = false
  form.comment = ''
  dialog.value = true
}

async function submit() {
  if (form.rework && form.reworkReason.trim().length < 5) return ElMessage.warning('返工原因不少于 5 个字')
  if (needConfirm.value && !form.rework && !form.secondConfirm) {
    return ElMessage.warning('面积偏差超过 3%，请勾选二次确认')
  }
  saving.value = true
  try {
    const res = await workApi.accept({
      orderId: pick.value.orderId || pick.value.id,
      acceptArea: form.acceptArea,
      qualityScore: form.qualityScore,
      reworkFlag: form.rework ? 1 : 0,
      reworkReason: form.rework ? form.reworkReason : null,
      secondConfirm: form.secondConfirm ? 1 : 0,
      comment: form.comment
    })
    ElMessage.success(res.result || '验收完成')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.strong { font-weight: 600; color: #19324a; }
.dim { color: #93a3b0; font-size: 12px; }
.danger { color: #b02a37; font-weight: 600; }
.imgs { display: flex; gap: 4px; }
</style>
