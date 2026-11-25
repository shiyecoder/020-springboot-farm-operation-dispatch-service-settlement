<template>
  <div>
    <div class="page-head">
      <div>
        <h2>保养维修</h2>
        <div class="sub">进行中的保养/维修会自动把机具置为不可派单状态，完成后恢复空闲并顺延下次保养日期</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">登记保养维修</el-button>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="机具编号" clearable @keyup.enter="search" />
        <el-select v-model="q.type" placeholder="类型" clearable>
          <el-option v-for="t in ['保养', '维修', '年检']" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="进行中" value="进行中" />
          <el-option label="已完成" value="已完成" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无保养维修记录">
        <el-table-column prop="machineNo" label="机具编号" min-width="110" />
        <el-table-column prop="maintainType" label="类型" min-width="86" />
        <el-table-column prop="content" label="作业内容" min-width="240" class-name="wrap" />
        <el-table-column label="费用(元)" min-width="100" align="right">
          <template #default="{ row }">{{ money(row.cost) }}</template>
        </el-table-column>
        <el-table-column label="起止日期" min-width="180">
          <template #default="{ row }">{{ formatDate(row.startDate) }} ~ {{ formatDate(row.endDate) }}</template>
        </el-table-column>
        <el-table-column prop="handler" label="经办人" min-width="90" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.maintainStatus === '已完成' ? 'success' : 'warning'">{{ row.maintainStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="附件" min-width="80">
          <template #default="{ row }">
            <el-image v-if="row.attachment" :src="row.attachment" :preview-src-list="[row.attachment]" fit="cover" class="thumb" preview-teleported />
            <span v-else class="dim">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑保养维修' : '登记保养维修'" width="520">
      <el-form :model="form" label-width="110px">
        <el-form-item label="机具" required>
          <el-select v-model="form.machineId" filterable style="width: 100%">
            <el-option v-for="m in machines" :key="m.id" :label="`${m.machineNo} ${m.machineName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.maintainType">
            <el-radio-button v-for="t in ['保养', '维修', '年检']" :key="t" :value="t">{{ t }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作业内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="费用(元)"><el-input-number v-model="form.cost" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.maintainStatus">
            <el-radio-button value="进行中">进行中</el-radio-button>
            <el-radio-button value="已完成">已完成</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="经办人"><el-input v-model="form.handler" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { machineApi } from '../api'
import { formatDate, money } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', type: '', status: '', page: 1, size: 10 })
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})
const machines = ref([])

async function load() {
  loading.value = true
  try {
    const res = await machineApi.maintainPage({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { maintainType: '保养', maintainStatus: '进行中', cost: 0, handler: '赵山河' })
  dialog.value = true
}

async function save() {
  if (!form.machineId) return ElMessage.warning('请选择机具')
  saving.value = true
  try {
    await machineApi.saveMaintain({ ...form })
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  machines.value = await machineApi.options()
  load()
})
</script>

<style scoped>
.dim { color: #b6c1ca; }
</style>
