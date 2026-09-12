import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'access_token'

const service = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

// ==================== 默认回调 ====================

const defaultFailure = (response: any) => {
  ElMessage.error(response.data?.message || '请求失败')
}

const defaultError = (error: any) => {
  const data = error?.response?.data
  if (data && typeof data === 'object' && data.code !== undefined && data.message) {
    ElMessage.error(data.message)
  } else {
    ElMessage.error('网络错误，请检查网络连接')
  }
  console.error(error)
}

// ==================== 通用响应处理 ====================

function handleResponse(
  response: any,
  onSuccess?: (res: any) => void,
  onFailure?: (res: any) => void,
) {
  if (response.data?.code === 200) {
    onSuccess ? onSuccess(response) : ElMessage.success(response.data?.message || '操作成功')
  } else {
    onFailure ? onFailure(response) : defaultFailure(response)
  }
}

// ==================== 内部请求 ====================

function internalPost(
  url: string, data?: any, headers?: Record<string, string>,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  service.post(url, data, { headers })
    .then(res => handleResponse(res, onSuccess, onFailure))
    .catch(err => onError ? onError(err) : defaultError(err))
}

function internalGet(
  url: string, params?: any, headers?: Record<string, string>,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  service.get(url, { params, headers })
    .then(res => handleResponse(res, onSuccess, onFailure))
    .catch(err => onError ? onError(err) : defaultError(err))
}

// ==================== 业务接口 ====================

function login(
  username: string, password: string,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  const form = new URLSearchParams()
  form.append('username', username)
  form.append('password', password)
  internalPost('/api/auth/login', form, { 'Content-Type': 'application/x-www-form-urlencoded' }, onSuccess, onFailure, onError)
}

function register(
  email: string, code: string, username: string, password: string,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalPost('/api/auth/register', { email, code, username, password }, undefined, onSuccess, onFailure, onError)
}

function resetConfirm(
  email: string, code: string,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalPost('/api/auth/reset-confirm', { email, code }, undefined, onSuccess, onFailure, onError)
}

function resetPassword(
  email: string, code: string, password: string,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalPost('/api/auth/reset-password', { email, code, password }, undefined, onSuccess, onFailure, onError)
}

// ==================== Token 管理 ====================

function storeAccessToken(token: string, remember: boolean) {
  (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token)
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

function takeAccessToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY)
  if (!token) return null

  try {
    const parts = token.split('.')
    if (parts.length !== 3) throw new Error()
    const payload = JSON.parse(atob(parts[1]!))
    if (payload.exp && Date.now() >= payload.exp * 1000) {
      clearToken()
      ElMessage.warning('登录已过期，请重新登录')
      return null
    }
    return token
  } catch {
    clearToken()
    return null
  }
}

function unauthorized(): boolean {
  return !takeAccessToken()
}

function accessHeader(): Record<string, string> {
  const token = takeAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function deleteAccessToken() {
  clearToken()
}

// ==================== 对外的请求方法（自动携带 Token） ====================

function post(
  url: string, data?: any,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalPost(url, data, accessHeader(), onSuccess, onFailure, onError)
}

function get(
  url: string, params?: any,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalGet(url, params, accessHeader(), onSuccess, onFailure, onError)
}

function put(
  url: string, data?: any,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  service.put(url, data, { headers: accessHeader() })
    .then(res => handleResponse(res, onSuccess, onFailure))
    .catch(err => onError ? onError(err) : defaultError(err))
}
function remove(
  url: string, onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  service.delete(url, { headers: accessHeader() }).then(res => handleResponse(res, onSuccess, onFailure)).catch(err => onError ? onError(err) : defaultError(err))
}

function aiChat(
  data: { conversationId?: string; message: string },
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  service.post('/api/ai/chat', data, { headers: accessHeader(), timeout: 30000 })
    .then(res => handleResponse(res, onSuccess, onFailure))
    .catch(err => onError ? onError(err) : defaultError(err))
}

// ==================== 验证码 ====================

function askCode(
  email: string, type: string,
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalGet('/api/auth/ask-code', { email, type }, undefined, onSuccess, onFailure, onError)
}

// ==================== 退出登录 ====================

function logout(
  onSuccess?: (res: any) => void, onFailure?: (res: any) => void, onError?: (err: any) => void,
) {
  internalPost('/api/auth/logout', null, accessHeader(),
    (res) => { clearToken(); ElMessage.success('退出登录成功'); onSuccess?.(res) },
    onFailure, onError,
  )
}

export { login, register, resetConfirm, resetPassword, logout, unauthorized, askCode,
  storeAccessToken, takeAccessToken, deleteAccessToken, accessHeader, post, get, put, remove, aiChat }
