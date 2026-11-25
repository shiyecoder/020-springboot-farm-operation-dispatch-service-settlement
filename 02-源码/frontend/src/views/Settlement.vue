<template>
  <div>
    <div class="page-head">
      <div>
        <h2>结算管理</h2>
        <div class="sub">应收按验收面积计算，支持现金 / 微信 / 银行转账 / 挂账；已收款完成的单据不可再改金额</div>
      </div>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="结算单号 / 派单号" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="收款状态" clearable>
          <el-option v-for="s in ['待收款', '部分收款', '已收款']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期"
                        value-format="YYYY-MM-DD" @change="onRange" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无结算单">
        <el-table-column prop="settleNo" label="结算单号" min-width="140" />
        <el-table-column label="派单 / 服务" min-width="170">
          <template #default="{ row }">
            <div class="strong">{{ row.orderNo }}</div>
            <div class="dim">{{ row.serviceType }} / {{ row.crop }} · {{ row.actualArea }} 亩</div>
          </template>
        </el-table-column>
        <el-table-column prop="farmerName" label="农户" min-width="90" />
        <el-table-column label="应收(元)" min-width="110" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.receivable) }}</span></template>
        </el-table-column>
        <el-table-column label="已收(元)" min-width="110" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.received) }}</span></template>
        </el-table-column>
        <el-table-column label="未收(元)" min-width="110" align="right">
          <template #default="{ row }">
            <span class="num" :class="{ danger: Number(row.unreceived) > 0 }">{{ money(row.unreceived) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="调整" min-width="96" align="right">
          <template #default="{ row }">{{ row.adjustAmount ? money(row.adjustAmount) : '—' }}</template>
        </el-table-column>
        <el-table-column prop="payMethod" label="收款方式" min-width="100" />
        <el-table-column label="收款状态" min-width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '已收款' ? 'success' : (row.status === '部分收款' ? 'warning' : 'danger')">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结算时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.settleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== '已收款'" link type="primary" @click="openPay(row)">登记收款</el-button>
            <el-button v-if="row.status !== '已收款'" link type="warning" @click="openAdjust(row)">减免</el-button>
            <span v-if="row.status === '已收款'" class="dim">已结清</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </div>

    <el-dialog v-model="payDialog" title="登记线下收款" width="460">
      <el-form label-width="100px">
        <el-form-item label="结算单">{{ pick?.settleNo }}</el-form-item>
        <el-form-item label="应收合计">{{ money(Number(pick?.receivable || 0) + Number(pick?.adjustAmount || 0)) }}</el-form-item>
        <el-form-item label="本次收款" required>
          <el-input-number v-model="payForm.received" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款方式" required>
          <el-radio-group v-model="payForm.payMethod">
            <el-radio-button v-for="p in ['现金', '微信', '银行转账', '挂账']" :key="p" :value="p">{{ p }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="收款凭证">
          <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
            <el-image v-if="payForm.voucherImage" :src="payForm.voucherImage" fit="cover" class="up-img" />
            <div v-else class="up-box">上传凭证</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="payForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doPay">确认收款</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustDialog" title="人工减免" width="420">
      <el-form label-width="100px">
        <el-form-item label="结算单">{{ pick?.settleNo }}</el-form-item>
        <el-form-item label="调整金额">
          <el-input-number v-model="adjustAmount" :precision="2" :step="10" style="width: 100%" />
          <div class="dim">负数表示减免，如 -50 表示减免 50 元</div>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="adjustReason" type="textarea" :rows="2" placeholder="如：地头漏耕减免 2%（不少于 5 个字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog = false">取消</el-button>
        <el-button type="primary" @click="doAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { settlementApi } from '../api'
import { formatTime, money } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', status: '', startDate: '', endDate: '', page: 1, size: 10 })
const range = ref(null)
const payDialog = ref(false)
const adjustDialog = ref(false)
const pick = ref(null)
const saving = ref(false)
const payForm = reactive({ received: 0, payMethod: '现金', voucherImage: '', remark: '' })
const adjustAmount = ref(0)
const adjustReason = ref('')

async function load() {
  loading.value = true
  try {
    const res = await settlementApi.page({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() { q.page = 1; load() }

function onRange(v) {
  q.startDate = v ? v[0] : ''
  q.endDate = v ? v[1] : ''
}

function openPay(row) {
  pick.value = row
  payForm.received = Number(row.unreceived || 0)
  payForm.payMethod = '现金'
  payForm.voucherImage = ''
  payForm.remark = ''
  payDialog.value = true
}

async function doPay() {
  saving.value = true
  try {
    await settlementApi.pay({
      id: pick.value.id, received: payForm.received,
      payMethod: payForm.payMethod, voucherImage: payForm.voucherImage, remark: payForm.remark
    })
    ElMessage.success('已登记收款')
    payDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openAdjust(row) {
  pick.value = row
  adjustAmount.value = 0
  adjustReason.value = ''
  adjustDialog.value = true
}

async function doAdjust() {
  if (adjustReason.value.trim().length < 5) return ElMessage.warning('调整原因不少于 5 个字')
  await settlementApi.adjust(pick.value.id, adjustAmount.value, adjustReason.value)
  ElMessage.success('已登记调整')
  adjustDialog.value = false
  load()
}

function upload(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fetch('/api/file/upload', { method: 'POST', headers: { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }, body: fd })
    .then((r) => r.json())
    .then((res) => {
      const d = res.data?.data || res.data || res
      payForm.voucherImage = d.path || d.url || d.filePath
      ElMessage.success('已上传')
    })
    .catch(() => ElMessage.error('上传失败'))
}

onMounted(load)
</script>

<style scoped>
.strong { font-weight: 600; color: #19324a; }
.dim { color: #93a3b0; font-size: 12px; }
.danger { color: #b02a37; font-weight: 600; }
.up-img { width: 90px; height: 68px; border-radius: 4px; object-fit: cover; border: 1px solid #e6ebef; }
.up-box { width: 90px; height: 68px; border: 1px dashed #c9d6dd; border-radius: 4px; display: grid; place-items: center; color: #93a3b0; font-size: 12px; }
</style>
