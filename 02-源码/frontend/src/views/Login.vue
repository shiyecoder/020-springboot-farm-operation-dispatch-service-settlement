<template>
  <div class="login-wrap">
    <div class="login-left">
      <div class="brand-line">
        <span class="mark">农</span>
        <span class="name">农机作业派单与服务结算管理系统</span>
      </div>
      <h1>让每一亩地<br />都排到合适的机具</h1>
      <p class="slogan">
        机具能力与成本联合派单，把资质、空闲时段、到场距离、油耗和准时率算清楚，
        调度员看得懂依据，农户看得清进度。
      </p>
      <ul class="points">
        <li><b>6 项因子加权</b>能力 / 资质 / 空闲 / 距离 / 油耗 / 准时率</li>
        <li><b>全流程留痕</b>报价、派单、作业、验收、结算逐步可追溯</li>
        <li><b>线下收款登记</b>现金、微信、转账、挂账四种方式</li>
      </ul>
    </div>

    <div class="login-right">
      <div class="card">
        <h2>账号登录</h2>
        <p class="tip">请使用合作社分配的账号登录，连续 5 次失败将锁定 10 分钟</p>
        <el-form :model="form" @submit.prevent>
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>
        <div class="demo">
          <span>演示账号</span>
          <div class="demo-list">
            <button v-for="d in demos" :key="d.u" type="button" @click="fill(d)">
              <b>{{ d.n }}</b><em>{{ d.u }}</em>
            </button>
          </div>
          <p class="demo-note">密码统一 123456，管理员 admin / admin123</p>
        </div>
        <p class="foot">农机合作社内部系统 · 建议使用 Chrome / Edge 访问</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const demos = [
  { n: '管理员', u: 'admin' }, { n: '调度员', u: 'dispatch01' },
  { n: '农机手', u: 'op01' }, { n: '机务', u: 'maint01' },
  { n: '财务', u: 'fin01' }, { n: '农户', u: 'farm01' }
]

function fill(d) {
  form.username = d.u
  form.password = d.u === 'admin' ? 'admin123' : '123456'
}

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号与密码')
  loading.value = true
  try {
    const res = await authApi.login({ ...form })
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(res.user))
    ElMessage.success('登录成功')
    // 农户默认进入门户
    router.push(res.user.roleCode === 'FARMER' ? '/portal' : '/dashboard')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { display: flex; min-height: 100vh; background: #f4f6f8; }
/* 左侧品牌区：真实农田航拍底图 + 数据钢蓝叠加，避免纯色块显得像模板 */
.login-left {
  flex: 1.05;
  background-image:
    linear-gradient(148deg, rgba(17, 36, 55, 0.93) 0%, rgba(21, 44, 66, 0.78) 44%, rgba(24, 50, 74, 0.42) 100%),
    url('../assets/login-bg.jpg');
  background-size: cover;
  background-position: center 62%;
  color: #fff; padding: 56px 60px; position: relative; overflow: hidden; display: flex; flex-direction: column;
}
.brand-line { display: flex; align-items: center; gap: 10px; margin-bottom: 46px; }
.mark {
  width: 30px; height: 30px; border-radius: 6px; background: #4d8a99; display: grid; place-items: center;
  font-weight: 700;
}
.name { font-size: 14px; letter-spacing: 1px; color: #d8e6ee; }
.login-left h1 { font-size: 34px; line-height: 1.35; margin: 0 0 18px; font-weight: 600; letter-spacing: 1px; }
.slogan { max-width: 460px; color: #b6cbd8; line-height: 1.9; font-size: 13px; margin: 0 0 30px; }
.points { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.points li { color: #cddde7; font-size: 13px; }
.points b { color: #fff; margin-right: 8px; font-weight: 600; }
.login-right {
  flex: .95; display: grid; place-items: center; padding: 40px;
  background:
    radial-gradient(900px 420px at 82% -8%, #e7eef2 0%, rgba(244, 246, 248, 0) 62%),
    repeating-linear-gradient(90deg, rgba(25, 50, 74, .035) 0 1px, transparent 1px 84px),
    #f4f6f8;
}
.card {
  width: 100%; max-width: 380px; background: #fff; border: 1px solid #e6ebef; border-radius: 10px;
  padding: 30px 30px 20px; box-shadow: 0 18px 44px -26px rgba(25, 50, 74, .5);
}
.foot { margin: 16px 0 0; text-align: center; font-size: 11px; color: #a8b5c0; }
.card h2 { margin: 0 0 6px; font-size: 20px; color: #19324a; }
.tip { color: #8496a5; font-size: 12px; margin: 0 0 20px; line-height: 1.6; }
.submit { width: 100%; height: 42px; background: #19324a; border-color: #19324a; letter-spacing: 4px; }
.submit:hover { background: #23475c; border-color: #23475c; }
.demo { margin-top: 22px; border-top: 1px dashed #e6ebef; padding-top: 14px; }
.demo > span { font-size: 12px; color: #8496a5; }
.demo-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 10px 0 8px; }
.demo-list button {
  border: 1px solid #e2e9ee; background: #f8fafb; border-radius: 6px; padding: 6px 4px; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.demo-list button:hover { border-color: #356a7a; background: #f0f6f8; }
.demo-list b { font-size: 12px; color: #19324a; font-weight: 600; }
.demo-list em { font-size: 11px; color: #8496a5; font-style: normal; }
.demo-note { font-size: 11px; color: #9aa9b5; margin: 4px 0 0; }
@media (max-width: 900px) {
  .login-left { display: none; }
  .login-right { flex: 1; }
}
</style>
