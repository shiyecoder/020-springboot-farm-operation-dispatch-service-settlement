<template>
  <div>
    <div class="page-head"><div><h2>个人中心</h2><div class="sub">修改资料、上传头像与修改密码，头像保存后右上角立即同步</div></div></div>

    <div class="grid">
      <div class="panel">
        <div class="panel-title">基本资料</div>
        <div class="avatar-row">
          <el-avatar :size="64" :src="profile.avatar || ''">{{ (profile.realName || 'U').slice(0, 1) }}</el-avatar>
          <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
            <el-button size="small">更换头像</el-button>
          </el-upload>
          <span class="hint">支持 jpg/png，建议 1:1 正方形</span>
        </div>
        <el-form :model="profile" label-width="90px" style="margin-top: 12px">
          <el-form-item label="账号"><el-input :model-value="profile.username" disabled /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="profile.realName" maxlength="50" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="profile.phone" maxlength="20" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="profile.email" maxlength="80" /></el-form-item>
          <el-form-item label="角色"><el-input :model-value="ROLE_LABEL[profile.roleCode] || profile.roleCode" disabled /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
      </div>

      <div class="panel">
        <div class="panel-title">修改密码</div>
        <el-form :model="pwd" label-width="90px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="不少于 6 位" /></el-form-item>
          <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
        </el-form>
        <el-button type="primary" @click="changePwd">修改密码</el-button>

        <div class="panel-title" style="margin-top: 22px">账号安全</div>
        <ul class="safe">
          <li>密码使用 BCrypt 加密保存，任何接口都不会返回密码字段</li>
          <li>连续 5 次登录失败将锁定 10 分钟</li>
          <li>JWT 有效期 12 小时，过期后需重新登录</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { ROLE_LABEL } from '../utils/format'

const profile = reactive({ username: '', realName: '', phone: '', email: '', avatar: '', roleCode: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const saving = ref(false)

async function load() {
  const me = await authApi.me()
  Object.assign(profile, me.user)
}

async function saveProfile() {
  saving.value = true
  try {
    await authApi.updateProfile({
      realName: profile.realName, phone: profile.phone, email: profile.email, avatar: profile.avatar
    })
    const me = await authApi.me()
    localStorage.setItem('user', JSON.stringify(me.user))
    ElMessage.success('资料已保存，刷新页面后右上角头像同步')
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码不少于 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await authApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改')
  pwd.oldPassword = ''
  pwd.newPassword = ''
  pwd.confirm = ''
}

function uploadAvatar(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fetch('/api/file/upload', { method: 'POST', headers: { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }, body: fd })
    .then((r) => r.json())
    .then((res) => {
      const d = res.data?.data || res.data || res
      profile.avatar = d.path || d.url || d.filePath
      ElMessage.success('头像已上传，保存资料后生效')
    })
    .catch(() => ElMessage.error('上传失败'))
}

onMounted(load)
</script>

<style scoped>
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.avatar-row { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 11px; color: #93a3b0; }
.safe { margin: 0; padding-left: 18px; color: #5a6b7a; font-size: 12px; line-height: 2; }
@media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
</style>
