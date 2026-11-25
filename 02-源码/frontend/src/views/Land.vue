<template>
  <div>
    <div class="page-head">
      <div>
        <h2>我的地块</h2>
        <div class="sub">地块面积、作物与到合作社里程直接影响报价的路程附加费和推荐的距离得分</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增地块</el-button>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe empty-text="还没有地块，先新增一块地">
        <el-table-column label="照片" width="86">
          <template #default="{ row }">
            <el-image :src="row.image" :preview-src-list="[row.image]" fit="cover" class="thumb" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="landNo" label="地块编号" min-width="110" />
        <el-table-column prop="landName" label="地块名称" min-width="140" />
        <el-table-column label="面积(亩)" min-width="96" align="right">
          <template #default="{ row }"><span class="num">{{ num(row.area) }}</span></template>
        </el-table-column>
        <el-table-column prop="crop" label="当季作物" min-width="96" />
        <el-table-column prop="region" label="区域" min-width="100" />
        <el-table-column prop="address" label="位置" min-width="220" class-name="wrap" />
        <el-table-column label="里程(km)" min-width="96" align="right">
          <template #default="{ row }">{{ num(row.distanceKm) }}</template>
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑地块' : '新增地块'" width="500">
      <el-form :model="form" label-width="110px">
        <el-form-item label="地块名称" required><el-input v-model="form.landName" maxlength="60" /></el-form-item>
        <el-form-item label="面积(亩)" required>
          <el-input-number v-model="form.area" :min="0.1" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当季作物">
          <el-select v-model="form.crop" style="width: 100%">
            <el-option v-for="c in CROPS" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="地块位置"><el-input v-model="form.address" maxlength="150" /></el-form-item>
        <el-form-item label="距合作社(km)">
          <el-input-number v-model="form.distanceKm" :min="0" :precision="1" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地块照片">
          <el-upload :show-file-list="false" :http-request="upload" accept="image/*">
            <el-image v-if="form.image" :src="form.image" fit="cover" class="up-img" />
            <div v-else class="up-box">点击上传</div>
          </el-upload>
        </el-form-item>
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
import { Plus } from '@element-plus/icons-vue'
import { machineApi } from '../api'
import { CROPS, num } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10 })
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})

async function load() {
  loading.value = true
  try {
    const res = await machineApi.landPage({ ...q })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { landName: '', area: 1, crop: '小麦', region: '城郊片区', distanceKm: 5 })
  dialog.value = true
}

async function save() {
  if (!form.landName) return ElMessage.warning('地块名称必填')
  saving.value = true
  try {
    await machineApi.saveLand({ ...form })
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

function upload(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fetch('/api/file/upload', { method: 'POST', headers: { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }, body: fd })
    .then((r) => r.json())
    .then((res) => {
      const d = res.data?.data || res.data || res
      form.image = d.path || d.url || d.filePath
      ElMessage.success('已上传')
    })
    .catch(() => ElMessage.error('上传失败'))
}

onMounted(load)
</script>

<style scoped>
.up-img { width: 96px; height: 72px; border-radius: 4px; object-fit: cover; border: 1px solid #e6ebef; }
.up-box { width: 96px; height: 72px; border: 1px dashed #c9d6dd; border-radius: 4px; display: grid; place-items: center; color: #93a3b0; font-size: 12px; }
</style>
