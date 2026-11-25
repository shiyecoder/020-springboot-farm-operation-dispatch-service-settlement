<template>
  <div class="portal">
    <div class="hero">
      <div class="hero-text">
        <h2>你好，{{ user.realName || '农户' }}</h2>
        <p>在这里提交耕地、播种、植保、收割需求，确认报价后在「验收管理」验收作业，系统会自动生成结算单。</p>
        <div class="hero-actions">
          <el-button type="primary" @click="$router.push('/request')">提交作业需求</el-button>
          <el-button @click="$router.push('/land')">管理我的地块</el-button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="hs"><span>进行中需求</span><b>{{ stat.running }}</b></div>
        <div class="hs"><span>待验收作业</span><b>{{ stat.waitAccept }}</b></div>
        <div class="hs"><span>累计作业面积</span><b>{{ stat.area }} 亩</b></div>
      </div>
    </div>

    <div class="grid">
      <div class="panel">
        <div class="panel-title">我的地块<span class="hint">里程影响报价，请如实填写</span></div>
        <div class="land-list">
          <div v-for="l in lands" :key="l.id" class="land-card">
            <el-image :src="l.image" fit="cover" class="land-img" />
            <div class="land-info">
              <div class="land-name">{{ l.landName }}<span class="crop">{{ l.crop }}</span></div>
              <div class="land-meta">{{ l.area }} 亩 · 距合作社 {{ l.distanceKm }} km</div>
              <div class="land-addr">{{ l.address }}</div>
            </div>
          </div>
          <div v-if="!lands.length" class="empty-note">还没有地块，点击「管理我的地块」新增</div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">最近需求<span class="hint">状态变化会同步到顶部消息</span></div>
        <el-table :data="requests" size="small" empty-text="暂无需求">
          <el-table-column prop="requestNo" label="编号" min-width="130" />
          <el-table-column prop="serviceType" label="服务" min-width="80" />
          <el-table-column label="面积" min-width="76" align="right">
            <template #default="{ row }">{{ row.area }} 亩</template>
          </el-table-column>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" min-width="110">
            <template #default="{ row }">
              <el-button v-if="row.status === '待验收'" link type="primary" @click="$router.push('/acceptance')">去验收</el-button>
              <el-button v-else link type="primary" @click="$router.push('/request')">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">我能做什么</div>
      <div class="steps">
        <div class="step"><b>1</b><span>维护地块</span><em>面积、作物、里程</em></div>
        <div class="step"><b>2</b><span>提交需求</span><em>服务类型、希望日期、紧急度</em></div>
        <div class="step"><b>3</b><span>确认报价</span><em>单价×面积+路程+加急</em></div>
        <div class="step"><b>4</b><span>等待作业</span><em>调度员排程派单</em></div>
        <div class="step"><b>5</b><span>验收评价</span><em>确认面积与质量</em></div>
        <div class="step"><b>6</b><span>线下结算</span><em>现金/微信/转账/挂账</em></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { machineApi, requestApi, workApi } from '../api'
import { statusType } from '../utils/format'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const lands = ref([])
const requests = ref([])
const stat = reactive({ running: 0, waitAccept: 0, area: 0 })

onMounted(async () => {
  const l = await machineApi.landPage({ page: 1, size: 6 })
  lands.value = l.records
  const r = await requestApi.page({ page: 1, size: 8 })
  requests.value = r.records
  stat.running = r.records.filter((x) => !['已完成', '已取消', '已退回'].includes(x.status)).length
  const a = await workApi.acceptPage({ page: 1, size: 50, status: '待验收' })
  stat.waitAccept = a.total
  stat.area = r.records.reduce((s, x) => s + Number(x.area || 0), 0).toFixed(1)
})
</script>

<style scoped>
.hero {
  display: flex; justify-content: space-between; gap: 24px; align-items: center;
  background: linear-gradient(120deg, #19324a 0%, #2c5b6d 100%); color: #fff;
  border-radius: 8px; padding: 22px 26px; margin-bottom: 14px;
}
.hero-text h2 { margin: 0 0 6px; font-size: 20px; font-weight: 600; }
.hero-text p { margin: 0 0 14px; color: #c2d5df; font-size: 13px; max-width: 620px; line-height: 1.8; }
.hero-actions { display: flex; gap: 10px; }
.hero-stats { display: flex; gap: 26px; }
.hs { display: flex; flex-direction: column; gap: 4px; }
.hs span { font-size: 12px; color: #a9c2cf; }
.hs b { font-size: 20px; font-variant-numeric: tabular-nums; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.land-list { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.land-card { display: flex; gap: 10px; border: 1px solid #e6ebef; border-radius: 6px; overflow: hidden; }
.land-img { width: 84px; height: 66px; object-fit: cover; flex: 0 0 84px; }
.land-info { padding: 6px 8px 6px 0; min-width: 0; }
.land-name { font-size: 13px; font-weight: 600; color: #19324a; display: flex; gap: 6px; align-items: center; }
.crop { font-size: 11px; color: #356a7a; background: #eef3f6; border-radius: 3px; padding: 0 4px; font-weight: 400; }
.land-meta { font-size: 12px; color: #6b7c8c; margin-top: 3px; }
.land-addr { font-size: 11px; color: #9aa9b5; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.steps { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; }
.step { border: 1px dashed #dfe8ed; border-radius: 6px; padding: 10px; display: flex; flex-direction: column; gap: 3px; }
.step b { color: #356a7a; font-size: 13px; }
.step span { font-size: 13px; color: #19324a; font-weight: 600; }
.step em { font-style: normal; font-size: 11px; color: #93a3b0; }
@media (max-width: 1100px) {
  .grid { grid-template-columns: 1fr; }
  .hero { flex-direction: column; align-items: flex-start; }
  .steps { grid-template-columns: repeat(2, 1fr); }
  .land-list { grid-template-columns: 1fr; }
}
</style>
