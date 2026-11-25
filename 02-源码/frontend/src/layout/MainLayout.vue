<template>
  <div class="app-shell">
    <!-- 侧边栏：菜单按角色动态下发，折叠后只留图标 -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-mark">农</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>农机作业派单</strong>
          <span>服务结算管理平台</span>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="activeMenu" :collapse="collapsed" class="menu"
                 background-color="transparent" text-color="#c8d4de" active-text-color="#ffffff"
                 @select="go">
          <template v-for="dir in menuTree" :key="dir.id">
            <el-sub-menu v-if="dir.children && dir.children.length" :index="String(dir.id)">
              <template #title>
                <el-icon><component :is="iconOf(dir.icon)" /></el-icon>
                <span>{{ dir.menuName }}</span>
              </template>
              <el-menu-item v-for="m in dir.children" :key="m.id" :index="m.path">
                <el-icon><component :is="iconOf(m.icon)" /></el-icon>
                <template #title>{{ m.menuName }}</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="dir.path">
              <el-icon><component :is="iconOf(dir.icon)" /></el-icon>
              <template #title>{{ dir.menuName }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </aside>

    <div class="main">
      <!-- 顶栏：面包屑 / 全局消息 / 帮助 / 头像 -->
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-btn" title="折叠菜单" @click="collapsed = !collapsed">
            <el-icon><Fold /></el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ roleLabel }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '工作台' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-popover placement="bottom-end" :width="360" trigger="click">
            <template #reference>
              <button class="icon-btn" title="站内消息">
                <el-badge :value="unread" :hidden="unread === 0" :max="99">
                  <el-icon><Bell /></el-icon>
                </el-badge>
              </button>
            </template>
            <div class="msg-head">站内消息</div>
            <div v-if="!messages.length" class="msg-empty">暂无消息</div>
            <div v-for="m in messages" :key="m.id" class="msg-item" :class="{ read: m.isRead }" @click="readMsg(m)">
              <div class="msg-title">{{ m.title }}</div>
              <div class="msg-body">{{ m.content }}</div>
              <div class="msg-time">{{ formatTime(m.createTime) }}</div>
            </div>
          </el-popover>
          <button class="icon-btn" title="帮助" @click="helpVisible = true">
            <el-icon><QuestionFilled /></el-icon>
          </button>
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="28" :src="user.avatar || ''">{{ (user.realName || 'U').slice(0, 1) }}</el-avatar>
              <span class="user-name">{{ user.realName }}</span>
              <el-icon><CaretBottom /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>

    <el-dialog v-model="helpVisible" title="使用帮助" width="560">
      <div class="help">
        <p><b>作业闭环：</b>需求提交 → 调度报价 → 农户确认 → 智能排程派单 → 农机手现场作业 → 农户验收 → 结算收款。</p>
        <p><b>联合推荐：</b>排程台会按「能力匹配 / 资质有效期 / 空闲时段 / 到场距离 / 历史油耗 / 准时率」六项加权打分，权重在「派单规则配置」中维护，合计必须 100%。</p>
        <p><b>面积偏差：</b>实际面积与报价面积偏差超过 3% 时，验收需勾选二次确认，确认后才可生成结算单。</p>
        <p><b>线下收款：</b>系统只登记收款状态（现金 / 微信 / 银行转账 / 挂账），不接入线上支付。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, systemApi } from '../api'
import { formatTime, ROLE_LABEL } from '../utils/format'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const helpVisible = ref(false)
const menus = ref([])
const messages = ref([])
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const activeMenu = computed(() => route.path)
const roleLabel = computed(() => ROLE_LABEL[user.value.roleCode] || '用户')
const unread = computed(() => messages.value.filter((m) => m.isRead === 0).length)

// 目录 + 子菜单两级结构
const menuTree = computed(() => {
  const dirs = menus.value.filter((m) => !m.parentId || m.parentId === 0)
  return dirs.map((d) => ({ ...d, children: menus.value.filter((m) => m.parentId === d.id) }))
})

const ICONS = {
  dash: 'DataBoard', request: 'Document', quote: 'Tickets', dispatch: 'Promotion',
  work: 'Van', accept: 'Select', machine: 'Setting', operator: 'User', price: 'PriceTag',
  maintain: 'Tools', settle: 'Money', rule: 'Operation', user: 'UserFilled',
  role: 'Avatar', menu: 'Menu', assign: 'Checked', log: 'Tickets', portal: 'House',
  land: 'MapLocation', folder: 'Folder', box: 'Box', money: 'Money', setting: 'Setting', home: 'House'
}
function iconOf(icon) {
  return ICONS[icon] || 'Menu'
}

function go(path) {
  if (path && path !== route.path) router.push(path)
}

async function readMsg(m) {
  if (m.isRead === 0) {
    await systemApi.readMessage(m.id)
    m.isRead = 1
  }
  if (m.url) router.push(m.url)
}

function onCommand(cmd) {
  if (cmd === 'profile') return router.push('/profile')
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(async () => {
  try {
    const me = await authApi.me()
    user.value = me.user
    localStorage.setItem('user', JSON.stringify(me.user))
    menus.value = me.menus || []
    messages.value = await systemApi.messages()
  } catch (e) {
    /* 拦截器已提示 */
  }
})
</script>

<style scoped>
.app-shell { display: flex; min-height: 100vh; background: #f4f6f8; }
.sidebar {
  width: 220px; background: #19324a; display: flex; flex-direction: column;
  transition: width .18s ease; flex: 0 0 220px;
}
.sidebar.collapsed { width: 64px; flex-basis: 64px; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 16px; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand-mark {
  width: 28px; height: 28px; border-radius: 6px; background: #356a7a; color: #fff;
  display: grid; place-items: center; font-weight: 700; font-size: 14px; flex: 0 0 28px;
}
.brand-text { display: flex; flex-direction: column; line-height: 1.25; overflow: hidden; white-space: nowrap; }
.brand-text strong { color: #fff; font-size: 14px; letter-spacing: .5px; }
.brand-text span { color: #8fa6b8; font-size: 11px; }
.menu-scroll { flex: 1; overflow: hidden; }
.menu { border-right: none; }
.menu :deep(.el-sub-menu__title), .menu :deep(.el-menu-item) { height: 42px; line-height: 42px; font-size: 13px; }
.menu :deep(.el-menu-item.is-active) { background: #2c5b6d; }
.menu :deep(.el-menu-item:hover), .menu :deep(.el-sub-menu__title:hover) { background: #223f58; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar {
  height: 56px; background: #fff; border-bottom: 1px solid #e6ebef;
  display: flex; align-items: center; justify-content: space-between; padding: 0 20px 0 12px;
}
.topbar-left { display: flex; align-items: center; gap: 14px; }
.topbar-right { display: flex; align-items: center; gap: 14px; }
.icon-btn {
  width: 32px; height: 32px; border: none; background: transparent; border-radius: 6px;
  display: grid; place-items: center; cursor: pointer; color: #45596b;
}
.icon-btn:hover { background: #eef2f5; }
.icon-btn svg { width: 18px; height: 18px; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #19324a; outline: none; }
.user-name { font-size: 13px; }
.content { flex: 1; padding: 24px; overflow: auto; }
.msg-head { font-weight: 600; color: #19324a; padding-bottom: 8px; border-bottom: 1px solid #eef2f5; }
.msg-item { padding: 8px 0; border-bottom: 1px dashed #eef2f5; cursor: pointer; }
.msg-item.read { opacity: .55; }
.msg-title { font-size: 13px; color: #19324a; font-weight: 600; }
.msg-body { font-size: 12px; color: #5a6b7a; margin-top: 2px; }
.msg-time { font-size: 11px; color: #93a3b0; margin-top: 2px; }
.msg-empty { color: #93a3b0; font-size: 12px; padding: 12px 0; }
.help p { font-size: 13px; color: #35495c; line-height: 1.9; margin: 0 0 6px; }
</style>
