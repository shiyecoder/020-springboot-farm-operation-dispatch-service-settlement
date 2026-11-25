<template>
  <div>
    <div class="page-head">
      <div>
        <h2>{{ titles[tab] || '系统管理' }}</h2>
        <div class="sub">菜单按角色动态下发，只有菜单权限不能直接调用写接口，数据范围由后端二次校验</div>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-change="onTab">
      <el-tab-pane label="用户管理" name="user" />
      <el-tab-pane label="角色管理" name="role" />
      <el-tab-pane label="菜单管理" name="menu" />
      <el-tab-pane label="分配菜单" name="assign" />
      <el-tab-pane label="操作日志" name="log" />
    </el-tabs>

    <!-- 用户 -->
    <div v-if="tab === 'user'" class="panel">
      <div class="filter-bar" style="margin-bottom: 12px">
        <el-input v-model="userQ.keyword" placeholder="账号 / 姓名" clearable style="width: 200px" @keyup.enter="loadUsers" />
        <el-select v-model="userQ.roleCode" placeholder="角色" clearable style="width: 180px">
          <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
        <el-button :icon="Plus" @click="openUser()">新增用户</el-button>
      </div>
      <el-table :data="users" size="small" stripe v-loading="loading">
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">{{ ROLE_LABEL[row.roleCode] || row.roleCode }}</template>
        </el-table-column>
        <el-table-column prop="orgName" label="组织" min-width="150" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="dataScope" label="数据范围" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="80" />
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openUser(row)">编辑</el-button>
            <el-button link type="warning" @click="reset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="userTotal"
                       v-model:current-page="userQ.page" :page-size="userQ.size" @change="loadUsers" />
      </div>
    </div>

    <!-- 角色 -->
    <div v-if="tab === 'role'" class="panel">
      <el-table :data="roles" size="small" stripe>
        <el-table-column prop="roleCode" label="角色编码" min-width="130" />
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column prop="dataScope" label="默认数据范围" min-width="130" />
        <el-table-column prop="remark" label="说明" min-width="240" class-name="wrap" />
      </el-table>
    </div>

    <!-- 菜单 -->
    <div v-if="tab === 'menu'" class="panel">
      <el-table :data="menus" size="small" row-key="id" default-expand-all
                :tree-props="{ children: 'children' }" empty-text="暂无菜单">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="path" label="路由" min-width="160" />
        <el-table-column prop="icon" label="图标" min-width="90" />
        <el-table-column prop="menuType" label="类型" min-width="80" />
        <el-table-column prop="sort" label="排序" min-width="70" />
        <el-table-column label="可见" min-width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="hint" style="margin-top: 8px">「菜单管理」本身按需求隐藏，不在侧边栏展示</div>
    </div>

    <!-- 分配菜单 -->
    <div v-if="tab === 'assign'" class="panel">
      <div class="filter-bar" style="margin-bottom: 12px">
        <el-select v-model="assignRole" placeholder="选择角色" style="width: 200px" @change="loadRoleMenus">
          <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
        </el-select>
        <el-button type="primary" :disabled="!assignRole" @click="saveAssign">保存授权</el-button>
      </div>
      <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="id" default-expand-all
               :props="{ label: 'menuName', children: 'children' }" />
    </div>

    <!-- 日志 -->
    <div v-if="tab === 'log'" class="panel">
      <el-table :data="logs" size="small" stripe v-loading="loading">
        <el-table-column prop="username" label="操作人" min-width="110" />
        <el-table-column prop="module" label="模块" min-width="110" />
        <el-table-column prop="action" label="操作" min-width="180" />
        <el-table-column prop="method" label="请求" min-width="240" class-name="wrap" />
        <el-table-column prop="ip" label="IP" min-width="120" />
        <el-table-column prop="costMs" label="耗时(ms)" min-width="90" align="right" />
        <el-table-column prop="result" label="结果" min-width="80" />
        <el-table-column label="时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="logTotal"
                       v-model:current-page="logPage" :page-size="10" @change="loadLogs" />
      </div>
    </div>

    <el-dialog v-model="userDialog" :title="userForm.id ? '编辑用户' : '新增用户'" width="460">
      <el-form :model="userForm" label-width="90px">
        <el-form-item label="账号" required>
          <el-input v-model="userForm.username" :disabled="!!userForm.id" maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="userForm.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleCode" style="width: 100%">
            <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="组织">
          <el-select v-model="userForm.orgId" style="width: 100%">
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="userForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio-button value="启用">启用</el-radio-button>
            <el-radio-button value="停用">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!userForm.id" label="初始密码">
          <el-input v-model="userForm.password" placeholder="留空默认 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { systemApi } from '../api'
import { ROLE_LABEL, formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const tab = ref(route.params.tab || 'user')
const titles = { user: '用户管理', role: '角色管理', menu: '菜单管理', assign: '为角色分配菜单', log: '操作日志' }

const users = ref([])
const userTotal = ref(0)
const userQ = reactive({ keyword: '', roleCode: '', page: 1, size: 10 })
const roles = ref([])
const orgs = ref([])
const menus = ref([])
const menuTree = ref([])
const logs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const loading = ref(false)
const userDialog = ref(false)
const userForm = reactive({})
const assignRole = ref('')
const treeRef = ref(null)

function buildTree(list) {
  const dirs = list.filter((m) => !m.parentId || m.parentId === 0)
  return dirs.map((d) => ({ ...d, children: list.filter((m) => m.parentId === d.id) }))
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await systemApi.users({ ...userQ })
    users.value = res.records
    userTotal.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await systemApi.logs({ page: logPage.value, size: 10 })
    logs.value = res.records
    logTotal.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadRoleMenus() {
  const ids = await systemApi.roleMenus(assignRole.value)
  treeRef.value?.setCheckedKeys(ids)
}

async function saveAssign() {
  const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await systemApi.saveRoleMenus({ roleCode: assignRole.value, menuIds: ids })
  ElMessage.success('授权已保存，被授权用户重新登录后生效')
}

function openUser(row) {
  Object.keys(userForm).forEach((k) => delete userForm[k])
  Object.assign(userForm, row || { username: '', realName: '', roleCode: 'FARMER', orgId: 1, status: '启用' })
  userDialog.value = true
}

async function saveUser() {
  if (!userForm.username) return ElMessage.warning('账号必填')
  await systemApi.saveUser({ ...userForm })
  ElMessage.success('已保存')
  userDialog.value = false
  loadUsers()
}

async function reset(row) {
  await ElMessageBox.confirm(`将 ${row.username} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await systemApi.resetPassword(row.id)
  ElMessage.success('已重置为 123456')
}

function onTab(name) {
  router.replace(`/system/${name}`)
  if (name === 'user') loadUsers()
  if (name === 'log') loadLogs()
  if (name === 'assign' && assignRole.value) loadRoleMenus()
}

watch(() => route.params.tab, (v) => { if (v && v !== tab.value) tab.value = v })

onMounted(async () => {
  roles.value = await systemApi.roles()
  orgs.value = await systemApi.orgs()
  menus.value = await systemApi.menus()
  menuTree.value = buildTree(menus.value)
  if (tab.value === 'user') loadUsers()
  if (tab.value === 'log') loadLogs()
})
</script>

<style scoped>
.hint { font-size: 11px; color: #93a3b0; }
</style>
