<template>
  <div>
    <div class="page-head">
      <div>
        <h2>农机手档案</h2>
        <div class="sub">操作证过期或剩余不足 30 天的农机手不可派单；评分与准时率参与联合推荐计算</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增农机手</el-button>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="编号 / 姓名 / 电话" clearable @keyup.enter="search" />
        <el-select v-model="q.certType" placeholder="可驾机型" clearable>
          <el-option v-for="t in MACHINE_TYPES" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option v-for="s in ['在岗', '请假', '停用']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="没有符合条件的农机手">
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <el-avatar :size="34" :src="row.avatar || ''">{{ (row.realName || '机').slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="operatorNo" label="编号" min-width="100" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="phone" label="电话" min-width="120" />
        <el-table-column prop="certTypes" label="可驾机型" min-width="150" />
        <el-table-column label="操作证有效期" min-width="150">
          <template #default="{ row }">
            <span>{{ formatDate(row.certExpire) }}</span>
            <el-tag size="small" :type="certState(row.certExpire).type" style="margin-left: 6px">
              {{ certState(row.certExpire).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="准时率" min-width="90" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.ontimeRate) }}%</span></template>
        </el-table-column>
        <el-table-column label="评分" min-width="76" align="center">
          <template #default="{ row }">{{ row.rating }}</template>
        </el-table-column>
        <el-table-column label="累计工时" min-width="96" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.totalHours) }}</span></template>
        </el-table-column>
        <el-table-column prop="region" label="常驻区域" min-width="100" />
        <el-table-column label="状态" min-width="86">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '在岗' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>

    <el-drawer v-model="dialog" :title="form.id ? '编辑农机手' : '新增农机手'" size="480px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="编号" required><el-input v-model="form.operatorNo" maxlength="50" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="可驾机型">
          <el-select v-model="certTypes" multiple style="width: 100%">
            <el-option v-for="t in MACHINE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="驾驶证号"><el-input v-model="form.licenseNo" maxlength="100" /></el-form-item>
        <el-form-item label="驾驶证有效期">
          <el-date-picker v-model="form.licenseExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="操作证有效期">
          <el-date-picker v-model="form.certExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="常驻区域"><el-input v-model="form.region" maxlength="100" /></el-form-item>
        <el-form-item label="准时率(%)"><el-input-number v-model="form.ontimeRate" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item label="评分"><el-input-number v-model="form.rating" :min="0" :max="5" :precision="2" :step="0.1" style="width: 100%" /></el-form-item>
        <el-form-item label="累计工时"><el-input-number v-model="form.totalHours" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="小时工资"><el-input-number v-model="form.hourlyWage" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.currentStatus">
            <el-radio-button value="在岗">在岗</el-radio-button>
            <el-radio-button value="请假">请假</el-radio-button>
            <el-radio-button value="停用">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { machineApi } from '../api'
import { MACHINE_TYPES, certState, formatDate, num } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', certType: '', status: '', page: 1, size: 10 })
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})

const certTypes = computed({
  get: () => (form.certTypes ? form.certTypes.split(',').filter(Boolean) : []),
  set: (v) => { form.certTypes = v.join(',') }
})

async function load() {
  loading.value = true
  try {
    const res = await machineApi.operatorPage({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row, status: undefined, currentStatus: row.status } : {
    operatorNo: '', realName: '', phone: '', certTypes: '', region: '城郊片区',
    ontimeRate: 95, rating: 5, totalHours: 0, hourlyWage: 55, currentStatus: '在岗'
  })
  dialog.value = true
}

async function save() {
  if (!form.operatorNo || !form.realName) return ElMessage.warning('编号与姓名必填')
  saving.value = true
  try {
    await machineApi.saveOperator({ ...form })
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
