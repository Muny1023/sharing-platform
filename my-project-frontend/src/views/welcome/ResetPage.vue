<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { askCode, resetConfirm, resetPassword } from '@/net'

const router = useRouter()

// ==================== 进度控制 ====================

const currentStep = ref(1)

function goStep2() {
  if (!validateStep1()) return
  resetConfirm(
    email.value.trim(), code.value.trim(),
    () => {
      currentStep.value = 2
    },
  )
}

// ==================== 表单数据 ====================

const email = ref('')
const code = ref('')
const newPassword = ref('')
const repeatPassword = ref('')

const emailError = ref('')
const codeError = ref('')
const newPasswordError = ref('')
const repeatPasswordError = ref('')

const showNewPassword = ref(false)
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

// ==================== 单字段校验 ====================

function validateEmailField(value: string): string {
  if (!value) return '电子邮件地址不能为空'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return '请输入合法的电子邮件地址'
  return ''
}

function validateCodeField(value: string): string {
  if (!value.trim()) return '验证码不能为空'
  return ''
}

function validateNewPasswordField(value: string): string {
  if (!value) return '新密码不能为空'
  if (value.length < 6 || value.length > 20) return '密码必须在6-20个字符之间'
  return ''
}

function validateRepeatPasswordField(value: string): string {
  if (!value) return '请再次输入密码'
  if (value !== newPassword.value) return '两次输入的密码不一致'
  return ''
}

// ==================== 分步校验 ====================

function clearErrors() {
  emailError.value = ''
  codeError.value = ''
  newPasswordError.value = ''
  repeatPasswordError.value = ''
}

function validateStep1(): boolean {
  clearErrors()
  let valid = true

  const eErr = validateEmailField(email.value.trim())
  if (eErr) { emailError.value = eErr; valid = false }

  const cErr = validateCodeField(code.value)
  if (cErr) { codeError.value = cErr; valid = false }

  return valid
}

function validateStep2(): boolean {
  clearErrors()
  let valid = true

  const pErr = validateNewPasswordField(newPassword.value)
  if (pErr) { newPasswordError.value = pErr; valid = false }

  const rpErr = validateRepeatPasswordField(repeatPassword.value)
  if (rpErr) { repeatPasswordError.value = rpErr; valid = false }

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
    email.value.trim(), 'reset',
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

function handleReset() {
  if (!validateStep2()) return
  resetPassword(
    email.value.trim(), code.value.trim(), newPassword.value,
    () => {
      ElMessage.success('密码重置成功')
      router.push('/')
    },
  )
}

function goLogin() {
  router.push('/')
}
</script>

<template>
  <div class="reset-wrapper">
    <div class="reset-card">
      <h1 class="reset-title">重置密码</h1>

      <el-steps :active="currentStep - 1" align-center class="reset-steps">
        <el-step title="验证邮箱" />
        <el-step title="重置密码" />
      </el-steps>

      <!-- ========== 步骤1：验证邮箱 ========== -->
      <form v-show="currentStep === 1" class="reset-form" @submit.prevent>
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

        <button type="submit" class="reset-btn" @click="goStep2">
          开始重置密码
        </button>
      </form>

      <!-- ========== 步骤2：重置密码 ========== -->
      <form v-show="currentStep === 2" class="reset-form" @submit.prevent>
        <div class="form-group">
          <div class="password-wrapper">
            <input
              v-model="newPassword"
              :type="showNewPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ 'is-error': newPasswordError }"
              placeholder="新密码"
              autocomplete="new-password"
              @input="newPasswordError = ''"
            />
            <button
              type="button"
              class="toggle-password"
              @click="showNewPassword = !showNewPassword"
            >
              {{ showNewPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <p v-if="newPasswordError" class="field-error">{{ newPasswordError }}</p>
        </div>

        <div class="form-group">
          <div class="password-wrapper">
            <input
              v-model="repeatPassword"
              :type="showRepeatPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ 'is-error': repeatPasswordError }"
              placeholder="再次输入密码"
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

        <button type="submit" class="reset-btn" @click="handleReset">
          立即重置
        </button>

        <p class="back-link">
          <a href="#" @click.prevent="currentStep = 1">← 返回上一步</a>
        </p>
      </form>

      <p class="login-link">
        想起密码了？
        <a href="#" @click.prevent="goLogin">立即登录</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.reset-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.reset-card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.reset-title {
  margin: 0 0 32px 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;
}

.reset-steps {
  margin-bottom: 32px;
}

.reset-form {
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

.reset-btn {
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

.reset-btn:hover {
  opacity: 0.9;
}

.reset-btn:active {
  transform: scale(0.98);
}

.back-link {
  margin: 0;
  text-align: center;
}

.back-link a {
  font-size: 13px;
  color: #888;
  text-decoration: none;
}

.back-link a:hover {
  color: #667eea;
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
