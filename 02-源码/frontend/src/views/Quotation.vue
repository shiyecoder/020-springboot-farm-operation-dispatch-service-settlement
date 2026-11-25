<template>
  <div>
    <div class="page-head">
      <div>
        <h2>报价确认</h2>
        <div class="sub">报价由后端按「单价 × 面积 + 路程附加费 + 加急附加费 + 其他附加」计算，调度员只填附加项</div>
      </div>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="需求编号 / 农户" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="需求状态" clearable>
          <el-option label="待报价" value="待报价" />
          <el-option label="待确认" value="待确认" />
          <el-option label="待排程" value="待排程" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="没有待报价的需求">
        <el-table-column prop="requestNo" label="需求编号" min-width="150" />
        <el-table-column prop="farmerName" label="农户" min-width="90" />
        <el-table-column prop="landName" label="地块" min-width="130" />
        <el-table-column prop="serviceType" label="服务类型" min-width="90" />
        <el-table-column prop="crop" label="作物" min-width="80" />
        <el-table-column label="面积(亩)" min-width="90" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.area) }}</span></template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急度" min-width="86" />
        <el-table-column label="状态" min-width="96">
          <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="金额(元)" min-width="110" align="right">
          <template #default="{ row }">{{ row.totalAmount > 0 ? money(row.totalAmount) : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待报价'" link type="primary" @click="openQuote(row)">生成报价</el-button>
            <el-button v-if="row.status === '待确认'" link type="primary" @click="confirm(row)">农户确认</el-button>
            <el-button v-if="['待报价', '待确认'].includes(row.status)" link type="danger" @click="back(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" title="生成报价" width="560">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 14px">
        <el-descriptions-item label="需求编号">{{ current?.requestNo }}</el-descriptions-item>
        <el-descriptions-item label="农户">{{ current?.farmerName }}</el-descriptions-item>
        <el-descriptions-item label="服务类型">{{ current?.serviceType }} / {{ current?.crop }}</el-descriptions-item>
        <el-descriptions-item label="面积">{{ current?.area }} 亩</el-descriptions-item>
        <el-descriptions-item label="紧急度">{{ current?.urgency }}</el-descriptions-item>
        <el-descriptions-item label="地块">{{ current?.landName }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="110px">
        <el-form-item label="其他附加费(元)">
          <el-input-number v-model="quote.extraFee" :min="0" :step="10" style="width: 180px" />
          <span class="hint">如秸秆清理、二次进场等</span>
        </el-form-item>
        <el-form-item label="报价说明">
          <el-input v-model="quote.remark" type="textarea" :rows="2" maxlength="120" />
        </el-form-item>
      </el-form>
      <el-result v-if="quoteResult" icon="success" title="报价已生成">
        <template #sub-title>
          <div class="breakdown">
            <div v-for="b in quoteResult.breakdown" :key="b.name" class="bd-row">
              <span>{{ b.name }}</span><em>{{ b.formula }}</em><b>{{ money(b.amount) }}</b>
            </div>
            <div class="bd-total">合计 <b>{{ money(quoteResult.quotation.totalAmount) }}</b></div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button @click="dialog = false">关闭</el-button>
        <el-button v-if="!quoteResult" type="primary" :loading="saving" @click="submitQuote">生成并发送给农户</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { requestApi } from '../api'
import { money, num, statusType } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', status: '', page: 1, size: 10 })
const dialog = ref(false)
const current = ref(null)
const quote = reactive({ extraFee: 0, remark: '' })
const quoteResult = ref(null)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await requestApi.page({ ...q, status: q.status || '待报价' })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }
function reset() { Object.assign(q, { keyword: '', status: '', page: 1 }); load() }

function openQuote(row) {
  current.value = row
  quote.extraFee = 0
  quote.remark = ''
  quoteResult.value = null
  dialog.value = true
}

async function submitQuote() {
  saving.value = true
  try {
    quoteResult.value = await requestApi.generateQuote({
      requestId: current.value.id, extraFee: quote.extraFee, remark: quote.remark
    })
    ElMessage.success('报价已生成')
    load()
  } finally {
    saving.value = false
  }
}

async function confirm(row) {
  await ElMessageBox.confirm(`确认农户已接受报价 ${money(row.totalAmount)}？`, '农户确认报价', { type: 'warning' })
  await requestApi.confirmQuote(row.id)
  ElMessage.success('已确认，进入待排程')
  load()
}

async function back(row) {
  const { value } = await ElMessageBox.prompt('请填写退回原因（不少于 5 个字）', '退回需求', {
    inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字'
  })
  await requestApi.back(row.id, value)
  ElMessage.success('已退回')
  load()
}

onMounted(load)
</script>

<style scoped>
.hint { font-size: 11px; color: #93a3b0; margin-left: 8px; }
.breakdown { text-align: left; font-size: 12px; color: #45596b; }
.bd-row { display: flex; justify-content: space-between; gap: 10px; padding: 4px 0; border-bottom: 1px dashed #eef2f5; }
.bd-row em { font-style: normal; color: #93a3b0; flex: 1; text-align: left; margin-left: 8px; }
.bd-total { margin-top: 8px; text-align: right; font-size: 13px; }
.bd-total b { color: #19324a; font-size: 16px; }
</style>
