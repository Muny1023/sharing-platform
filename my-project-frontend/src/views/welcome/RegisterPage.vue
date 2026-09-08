<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { askCode, register } from '@/net'

const router = useRouter()

// ==================== 表单数据 ====================

const username = ref('')
const password = ref('')
const repeatPassword = ref('')
const email = ref('')
const code = ref('')

const usernameError = ref('')
const passwordError = ref('')
const repeatPasswordError = ref('')
const emailError = ref('')
const codeError = ref('')

const showPassword = ref(false)
const showRepeatPassword = ref(false)

// ==================== 验证码相关 ====================

const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const canSendCode = computed(() => countdown.value === 0 && !validateEmailField(email.value.trim()))

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      resetCountdown()
    }
  }, 1000)
}

function resetCountdown() {
  countdown.value = 0
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onUnmounted(() => resetCountdown())

// ==================== 单字段校验 ====================

function validateUsernameField(value: string): string {
  if (!value) return '用户名不能为空'
  if (value.length < 2 || value.length > 8) return '用户名必须在2-8个字符之间'
  if (!/^[一-龥a-zA-Z]+$/.test(value)) return '用户名只能包含中英文字符，不能包含特殊字符'
  return ''
}

function validatePasswordField(value: string): string {
  if (!value) return '密码不能为空'
  if (value.length < 6 || value.length > 20) return '密码必须在6-20个字符之间'
  return ''
}

function validateRepeatPasswordField(value: string): string {
  if (!value) return '重复密码不能为空'
  if (value !== password.value) return '两次输入的密码不一致'
  return ''
}

function validateEmailField(value: string): string {
  if (!value) return '电子邮件地址不能为空'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return '请输入合法的电子邮件地址'
  return ''
}

function validateCodeField(value: string): string {
  if (!value.trim()) return '验证码不能为空'
  return ''
}

// ==================== 全量校验 ====================

function clearErrors() {
  usernameError.value = ''
  passwordError.value = ''
  repeatPasswordError.value = ''
  emailError.value = ''
  codeError.value = ''
}

function validateAll(): boolean {
  clearErrors()
  let valid = true

  const uErr = validateUsernameField(username.value.trim())
  if (uErr) { usernameError.value = uErr; valid = false }

  const pErr = validatePasswordField(password.value)
  if (pErr) { passwordError.value = pErr; valid = false }

  const rpErr = validateRepeatPasswordField(repeatPassword.value)
  if (rpErr) { repeatPasswordError.value = rpErr; valid = false }

  const eErr = validateEmailField(email.value.trim())
  if (eErr) { emailError.value = eErr; valid = false }

  const cErr = validateCodeField(code.value)
  if (cErr) { codeError.value = cErr; valid = false }

  return valid
}

// ==================== 事件处理 ====================

function sendCode() {
  const eErr = validateEmailField(email.value.trim())
  if (eErr) {
    emailError.value = eErr
    return
  }

  askCode(
    email.value.trim(), 'register',
    () => {
      ElMessage.success('发送成功，请查收')
      startCountdown()
    },
    undefined,
    () => {
      resetCountdown()
      ElMessage.error('网络错误，请检查网络连接')
    },
  )
}

function handleRegister() {
  if (!validateAll()) return

  register(
    email.value.trim(), code.value.trim(), username.value.trim(), password.value,
    () => {
      ElMessage.success('注册成功')
      router.push('/')
    },
  )
}

function goLogin() {
  router.push('/')
}
</script>

<template>
  <div class="register-wrapper">
    <div class="register-card">
      <h1 class="register-title">注册新用户</h1>

      <form class="register-form" @submit.prevent>
        <div class="form-group">
          <input
            v-model="username"
            type="text"
            class="form-input"
            :class="{ 'is-error': usernameError }"
            placeholder="用户名"
            autocomplete="username"
            @input="usernameError = ''"
          />
          <p v-if="usernameError" class="field-error">{{ usernameError }}</p>
        </div>

        <div class="form-group">
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ 'is-error': passwordError }"
              placeholder="密码"
              autocomplete="new-password"
              @input="passwordError = ''"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <p v-if="passwordError" class="field-error">{{ passwordError }}</p>
        </div>

        <div class="form-group">
          <div class="password-wrapper">
            <input
              v-model="repeatPassword"
              :type="showRepeatPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ 'is-error': repeatPasswordError }"
              placeholder="重复密码"
              autocomplete="new-password"
              @input="repeatPasswordError = ''"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showRepeatPassword = !showRepeatPassword"
            >
              {{ showRepeatPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <p v-if="repeatPasswordError" class="field-error">{{ repeatPasswordError }}</p>
        </div>

        <div class="form-group">
          <input
            v-model="email"
            type="email"
            class="form-input"
            :class="{ 'is-error': emailError }"
            placeholder="电子邮件地址"
            autocomplete="email"
            @input="emailError = ''"
          />
          <p v-if="emailError" class="field-error">{{ emailError }}</p>
        </div>

        <div class="form-group code-group">
          <div class="code-field">
            <input
              v-model="code"
              type="text"
              class="form-input code-input"
              :class="{ 'is-error': codeError }"
              placeholder="请输入验证码"
              @input="codeError = ''"
            />
            <p v-if="codeError" class="field-error">{{ codeError }}</p>
          </div>
          <button
            type="button"
            class="send-code-btn"
            :disabled="!canSendCode"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
        </div>

        <button type="submit" class="register-btn" @click="handleRegister">
          立即注册
        </button>
      </form>

      <p class="login-link">
        已有账号？
        <a href="#" @click.prevent="goLogin">立即登录</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.register-title {
  margin: 0 0 32px 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  position: relative;
}

.password-wrapper {
  position: relative;
}

.password-wrapper .form-input {
  padding-right: 60px;
}

.toggle-password {
  position: absolute;
  right: 0;
  top: 0;
  height: 100%;
  padding: 0 12px;
  font-size: 13px;
  color: #888;
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.toggle-password:hover {
  color: #667eea;
}

.form-input {
  width: 100%;
  padding: 14px 16px;
  font-size: 15px;
  color: #333;
  background: #f5f6fa;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.25s, box-shadow 0.25s;
  box-sizing: border-box;
}

.form-input::placeholder {
  color: #b0b0b0;
}

.form-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #fff;
}

.form-input.is-error {
  border-color: #e74c3c;
}

.form-input.is-error:focus {
  box-shadow: 0 0 0 3px rgba(231, 76, 60, 0.12);
}

.field-error {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #e74c3c;
}

.code-group {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.code-field {
  flex: 1;
}

.send-code-btn {
  flex-shrink: 0;
  margin-top: 0;
  padding: 14px 16px;
  font-size: 13px;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.send-code-btn:hover:not(:disabled) {
  opacity: 0.85;
}

.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.register-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.25s, transform 0.15s;
}

.register-btn:hover {
  opacity: 0.9;
}

.register-btn:active {
  transform: scale(0.98);
}

.login-link {
  margin: 24px 0 0 0;
  font-size: 14px;
  color: #888;
  text-align: center;
}

.login-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
