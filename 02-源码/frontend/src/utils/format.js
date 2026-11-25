// 全局时间与数字格式化：全系统统一调用，禁止各页面重复实现
export function formatTime(v) {
  if (!v) return '—'
  const s = String(v).replace('T', ' ')
  return s.length >= 19 ? s.slice(0, 19) : s
}

export function formatDate(v) {
  if (!v) return '—'
  return String(v).slice(0, 10)
}

export function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function num(v, digits = 1) {
  if (v === null || v === undefined || v === '') return '—'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

/** 需求/派单状态 → 标签色 */
export function statusType(s) {
  return {
    待报价: 'info', 待确认: 'warning', 待排程: 'warning', 待作业: '',
    作业中: 'primary', 待验收: 'warning', 已完成: 'success',
    已退回: 'danger', 已取消: 'info', 待返工: 'danger'
  }[s] ?? 'info'
}

/** 推荐等级 → 标签色（推荐/可选/不推荐） */
export function levelType(l) {
  return { 推荐: 'success', 可选: 'warning', 不推荐: 'danger' }[l] || 'info'
}

export function certState(expire) {
  if (!expire) return { text: '未登记', type: 'info' }
  const today = new Date()
  const d = new Date(expire)
  const days = Math.floor((d - today) / 86400000)
  if (days < 0) return { text: '已过期', type: 'danger' }
  if (days <= 60) return { text: `剩余 ${days} 天`, type: 'warning' }
  return { text: '有效', type: 'success' }
}

export const ROLE_LABEL = {
  ADMIN: '系统管理员', DISPATCHER: '调度员', OPERATOR: '农机手',
  MAINT: '机务管理员', FINANCE: '财务人员', FARMER: '农户客户'
}

export const SERVICE_TYPES = ['深耕', '旋耕', '播种', '植保', '收割', '打捆']
export const CROPS = ['小麦', '玉米', '水稻', '苹果', '油菜']
export const MACHINE_TYPES = ['拖拉机', '收割机', '播种机', '植保无人机', '旋耕机', '打捆机']
export const MACHINE_STATUS = ['空闲', '作业中', '保养中', '维修中', '停用']
export const URGENCY = ['普通', '加急', '特急']
export const REQUEST_STATUS = ['待报价', '待确认', '待排程', '待作业', '作业中', '待验收', '已完成', '已退回', '已取消']
export const ORDER_STATUS = ['待作业', '作业中', '待验收', '已完成', '已取消', '待返工']
