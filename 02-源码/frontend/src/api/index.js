import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 统一 axios 实例：自动带令牌、统一错误提示、401 回登录页
const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(body.message))
    }
    return body.data
  },
  (err) => {
    const msg = err.response?.data?.message || (err.response?.status === 403 ? '无权限访问' : '网络异常，请稍后重试')
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default http

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  changePassword: (data) => http.post('/auth/password', data),
  updateProfile: (data) => http.put('/auth/profile', data)
}

export const dashboardApi = {
  overview: (period = 'month') => http.get('/dashboard', { params: { period } })
}

export const requestApi = {
  page: (params) => http.get('/request/page', { params }),
  detail: (id) => http.get(`/request/${id}`),
  save: (data) => http.post('/request', data),
  back: (id, reason) => http.post(`/request/${id}/back`, { reason }),
  cancel: (id, reason) => http.post(`/request/${id}/cancel`, { reason }),
  schedulable: () => http.get('/request/schedulable'),
  generateQuote: (data) => http.post('/quotation/generate', data),
  confirmQuote: (id) => http.post(`/quotation/${id}/confirm`)
}

export const dispatchApi = {
  candidates: (requestId) => http.get('/dispatch/candidates', { params: { requestId } }),
  evaluate: (data) => http.post('/dispatch/evaluate', data),
  dispatch: (data) => http.post('/dispatch/dispatch', data),
  orderPage: (params) => http.get('/dispatch/order/page', { params }),
  orderDetail: (id) => http.get(`/dispatch/order/${id}`),
  cancelOrder: (id, reason) => http.post(`/dispatch/order/${id}/cancel`, { reason })
}

export const workApi = {
  page: (params) => http.get('/work/page', { params }),
  start: (data) => http.post('/work/start', data),
  finish: (data) => http.post('/work/finish', data),
  acceptPage: (params) => http.get('/acceptance/page', { params }),
  accept: (data) => http.post('/acceptance/accept', data)
}

export const machineApi = {
  page: (params) => http.get('/machine/page', { params }),
  detail: (id) => http.get(`/machine/${id}`),
  save: (data) => http.post('/machine', data),
  changeStatus: (id, data) => http.post(`/machine/${id}/status`, data),
  options: () => http.get('/machine/options'),
  operatorPage: (params) => http.get('/operator/page', { params }),
  saveOperator: (data) => http.post('/operator', data),
  operatorOptions: () => http.get('/operator/options'),
  priceList: (keyword) => http.get('/price/list', { params: { keyword } }),
  savePrice: (data) => http.post('/price', data),
  landPage: (params) => http.get('/land/page', { params }),
  saveLand: (data) => http.post('/land', data),
  maintainPage: (params) => http.get('/maintain/page', { params }),
  saveMaintain: (data) => http.post('/maintain', data)
}

export const settlementApi = {
  page: (params) => http.get('/settlement/page', { params }),
  detail: (id) => http.get(`/settlement/${id}`),
  pay: (data) => http.post('/settlement/pay', data),
  adjust: (id, amount, reason) => http.post(`/settlement/${id}/adjust`, null, { params: { amount, reason } })
}

export const ruleApi = {
  list: () => http.get('/rule/list'),
  save: (data) => http.post('/rule', data),
  effect: () => http.get('/rule/effect'),
  scorePage: (params) => http.get('/score/page', { params }),
  scoreDetail: (id) => http.get(`/score/${id}`),
  adjust: (id, data) => http.post(`/score/${id}/adjust`, data)
}

export const systemApi = {
  users: (params) => http.get('/system/users', { params }),
  saveUser: (data) => http.post('/system/user', data),
  resetPassword: (id) => http.post(`/system/user/${id}/reset`),
  roles: () => http.get('/system/roles'),
  orgs: () => http.get('/system/orgs'),
  menus: () => http.get('/system/menus'),
  roleMenus: (roleCode) => http.get('/system/role-menus', { params: { roleCode } }),
  saveRoleMenus: (data) => http.post('/system/role-menus', data),
  messages: () => http.get('/system/messages'),
  readMessage: (id) => http.post(`/system/messages/${id}/read`),
  logs: (params) => http.get('/system/logs', { params })
}
