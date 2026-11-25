<template>
  <div class="dash">
    <div class="page-head">
      <div>
        <h2>机具运营看板</h2>
        <div class="sub">合作社作业面积、机具利用、完工时效与收款情况</div>
      </div>
      <el-radio-group v-model="period" size="small" @change="load">
        <el-radio-button value="30d">近30天</el-radio-button>
        <el-radio-button value="month">本月</el-radio-button>
        <el-radio-button value="quarter">本季度</el-radio-button>
        <el-radio-button value="year">本年</el-radio-button>
      </el-radio-group>
    </div>

    <div class="kpi-grid">
      <div v-for="k in kpis" :key="k.label" class="kpi-card">
        <div class="kpi-label">{{ k.label }}</div>
        <div class="kpi-value">{{ k.value }}<span class="kpi-unit">{{ k.unit }}</span></div>
        <div class="kpi-foot">{{ k.foot }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel chart-card wide">
        <div class="panel-title">近 6 个月作业面积<span class="hint">单位：亩</span></div>
        <EChart :option="trendOption" :height="176" />
      </div>
      <div class="panel chart-card">
        <div class="panel-title">服务类型面积分布</div>
        <EChart :option="typeOption" :height="176" />
      </div>
      <div class="panel chart-card">
        <div class="panel-title">派单状态分布</div>
        <EChart :option="statusOption" :height="176" />
      </div>
      <div class="panel chart-card">
        <div class="panel-title">机具派单量 TOP8</div>
        <EChart :option="machineOption" :height="176" />
      </div>
      <div class="panel chart-card">
        <div class="panel-title">农机手作业工时 TOP6<span class="hint">小时</span></div>
        <EChart :option="opOption" :height="176" />
      </div>
      <div class="panel chart-card">
        <div class="panel-title">联合推荐结果分布</div>
        <EChart :option="levelOption" :height="176" />
      </div>
    </div>

    <div class="panel todo-bar">
      <span class="todo-title">今日待办</span>
      <router-link to="/dispatch" class="todo-item">
        待排程需求 <b>{{ todo.waitDispatch }}</b> 单
      </router-link>
      <router-link to="/acceptance" class="todo-item">
        待验收作业 <b>{{ todo.waitAccept }}</b> 单
      </router-link>
      <router-link to="/settlement" class="todo-item">
        未收完结算 <b>{{ todo.waitPay }}</b> 单
      </router-link>
      <span class="grow"></span>
      <span class="todo-hint">图表点击可回到对应明细列表</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EChart from '../components/EChart.vue'
import { dashboardApi } from '../api'

const router = useRouter()
const period = ref('month')
const data = ref({ kpi: {}, trend: { months: [], area: [] }, serviceTypes: [], orderStatus: [],
  machineRank: [], operatorRank: [], scoreLevel: [], todo: {} })

const kpis = computed(() => {
  const k = data.value.kpi || {}
  return [
    { label: '累计作业面积', value: k.totalArea ?? 0, unit: '亩', foot: `共 ${k.orderCount ?? 0} 张派单` },
    { label: '机具利用率', value: k.utilization ?? 0, unit: '%', foot: '本期有派单机具 / 在册机具' },
    { label: '按时完工率', value: k.ontimeRate ?? 0, unit: '%', foot: '完工不晚于计划结束' },
    { label: '平均亩均油耗', value: k.avgFuel ?? 0, unit: '升/亩', foot: '按实际面积加权' },
    { label: '返工率', value: k.reworkRate ?? 0, unit: '%', foot: '验收要求返工占比' },
    { label: '应收完成率', value: k.receiveRate ?? 0, unit: '%', foot: `未收 ${k.unreceived ?? 0} 元` }
  ]
})

const todo = computed(() => data.value.todo || { waitDispatch: 0, waitAccept: 0, waitPay: 0 })

const palette = ['#19324a', '#356a7a', '#456c7e', '#7d9aa8', '#a8bcc6', '#c9d6dd']
const axis = {
  axisLine: { lineStyle: { color: '#e6ebef' } },
  axisLabel: { color: '#6b7c8c', fontSize: 10 },
  splitLine: { lineStyle: { color: '#f0f3f5' } }
}

const trendOption = computed(() => ({
  grid: { left: 40, right: 14, top: 18, bottom: 24 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: data.value.trend.months, ...axis },
  yAxis: { type: 'value', ...axis },
  series: [{
    type: 'line', smooth: true, symbolSize: 5,
    data: data.value.trend.area,
    itemStyle: { color: '#356a7a' },
    lineStyle: { width: 2 },
    areaStyle: { color: 'rgba(53,106,122,.12)' }
  }]
}))

function pie(list) {
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: '#6b7c8c' } },
    color: palette,
    series: [{
      type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'],
      label: { show: false }, data: list,
      itemStyle: { borderColor: '#fff', borderWidth: 1 }
    }]
  }
}

const typeOption = computed(() => pie(data.value.serviceTypes || []))
const statusOption = computed(() => pie(data.value.orderStatus || []))
const levelOption = computed(() => pie(data.value.scoreLevel || []))

const machineOption = computed(() => ({
  grid: { left: 74, right: 16, top: 10, bottom: 10 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', ...axis },
  yAxis: { type: 'category', data: (data.value.machineRank || []).map((x) => x.name).reverse(), ...axis },
  series: [{ type: 'bar', barWidth: 10, itemStyle: { color: '#456c7e' },
    data: (data.value.machineRank || []).map((x) => x.value).reverse() }]
}))

const opOption = computed(() => ({
  grid: { left: 60, right: 16, top: 10, bottom: 22 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (data.value.operatorRank || []).map((x) => x.name), ...axis },
  yAxis: { type: 'value', ...axis },
  series: [{ type: 'bar', barWidth: 14, itemStyle: { color: '#19324a' },
    data: (data.value.operatorRank || []).map((x) => x.value) }]
}))

async function load() {
  data.value = await dashboardApi.overview(period.value)
}

onMounted(load)
</script>

<style scoped>
.chart-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.chart-card { margin-bottom: 0; padding: 12px 14px; }
.chart-card.wide { grid-column: span 2; }
.panel-title { margin-bottom: 4px; font-size: 13px; }
.todo-bar { display: flex; align-items: center; gap: 20px; margin-top: 12px; padding: 10px 16px; }
.todo-title { font-weight: 600; color: #19324a; }
.todo-item { color: #45596b; font-size: 13px; text-decoration: none; }
.todo-item b { color: #b02a37; font-size: 15px; margin: 0 2px; }
.todo-item:hover { color: #356a7a; }
.grow { flex: 1; }
.todo-hint { color: #93a3b0; font-size: 12px; }
@media (max-width: 1200px) {
  .chart-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 768px) {
  .chart-grid { grid-template-columns: 1fr; }
  .chart-card.wide { grid-column: span 1; }
  .todo-bar { flex-wrap: wrap; gap: 10px; }
}
</style>
