<template>
  <div>
    <div class="page-head">
      <div>
        <h2>服务价目</h2>
        <div class="sub">按作业类型与作物维护基础单价、免费里程、超里程单价与加急系数；报价由后端据此计算</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增价目</el-button>
    </div>

    <div class="panel tight">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="作业类型 / 作物" clearable style="width: 220px" @keyup.enter="load" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="尚未配置价目">
        <el-table-column prop="serviceType" label="作业类型" min-width="110" />
        <el-table-column prop="crop" label="作物" min-width="100" />
        <el-table-column prop="unit" label="计费单位" min-width="90" />
        <el-table-column label="基础单价" min-width="110" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.basePrice) }}/亩</span></template>
        </el-table-column>
        <el-table-column label="免费里程" min-width="100" align="right">
          <template #default="{ row }">{{ num(row.freeKm) }} km</template>
        </el-table-column>
        <el-table-column label="超里程单价" min-width="120" align="right">
          <template #default="{ row }">{{ money(row.roadFeePerKm) }}/km</template>
        </el-table-column>
        <el-table-column label="加急系数" min-width="100" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.urgentRate) }}</span></template>
        </el-table-column>
        <el-table-column prop="priceStatus" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.priceStatus === '启用' ? 'success' : 'info'">{{ row.priceStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑价目' : '新增价目'" width="480">
      <el-form :model="form" label-width="110px">
        <el-form-item label="作业类型" required>
          <el-select v-model="form.serviceType" style="width: 100%">
            <el-option v-for="t in SERVICE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="作物">
          <el-select v-model="form.crop" style="width: 100%">
            <el-option v-for="c in CROPS" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础单价(元/亩)" required>
          <el-input-number v-model="form.basePrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="免费里程(km)">
          <el-input-number v-model="form.freeKm" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="超里程(元/km)">
          <el-input-number v-model="form.roadFeePerKm" :min="0" :precision="2" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加急系数">
          <el-input-number v-model="form.urgentRate" :min="1" :max="3" :precision="2" :step="0.05" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.priceStatus">
            <el-radio-button value="启用">启用</el-radio-button>
            <el-radio-button value="停用">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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
import { CROPS, SERVICE_TYPES, money, num } from '../utils/format'

const rows = ref([])
const keyword = ref('')
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})

async function load() {
  loading.value = true
  try {
    rows.value = await machineApi.priceList(keyword.value)
  } finally {
    loading.value = false
  }
}

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || {
    serviceType: '旋耕', crop: '小麦', unit: '亩', basePrice: 50,
    freeKm: 10, roadFeePerKm: 3.5, urgentRate: 1.15, priceStatus: '启用'
  })
  dialog.value = true
}

async function save() {
  if (!form.serviceType) return ElMessage.warning('作业类型必填')
  saving.value = true
  try {
    await machineApi.savePrice({ ...form })
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
