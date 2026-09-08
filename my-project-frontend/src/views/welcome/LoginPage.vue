<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, storeAccessToken } from '@/net'

const router = useRouter()
const username = ref('')
const password = ref('')
const rememberMe = ref(false)
const errorMessage = ref('')
const loading = ref(false)

function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    errorMessage.value = '用户名或密码不能为空'
    return
  }
  errorMessage.value = ''
  loading.value = true

  login(
    username.value,
    password.value,
    // 成功回调
    (response) => {
      loading.value = false
      const data = response.data?.data
      const token = data?.token
      const name = data?.username || username.value
      if (token) {
        storeAccessToken(token, rememberMe.value)
        ElMessage.success(`登录成功，欢迎 ${name} 进入系统`)
        router.push('/index')
      }
    },
    // 失败回调
    () => {
      loading.value = false
      ElMessage.error('用户名或密码错误')
    },
    // 错误回调（网络异常由 defaultError 自动弹出）
    () => {
      loading.value = false
    },
  )
}
</script>

<template>
  <div class="login-wrapper">
    <div class="login-card">
      <h1 class="login-title">登录</h1>
      <p class="login-desc">在进入系统之前，请先输入用户名和密码进行登录</p>

      <form class="login-form" @submit.prevent>
        <div class="form-group">
          <input
            v-model="username"
            type="text"
            class="form-input"
            placeholder="用户名/邮箱"
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <input
            v-model="password"
            type="password"
            class="form-input"
            placeholder="密码"
            autocomplete="current-password"
          />
        </div>

        <div class="form-extra">
          <label class="remember-me">
            <input v-model="rememberMe" type="checkbox" />
            <span>记住我</span>
          </label>
          <a href="#" class="forgot-link" @click.prevent="router.push('/reset')">忘记密码?</a>
        </div>

        <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>

        <button type="submit" class="login-btn" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
      </form>

      <p class="register-link">
        没有账号？
        <a href="#" @click.prevent="router.push('/register')">立即注册</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-title {
  margin: 0 0 12px 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;
}

.login-desc {
  margin: 0 0 32px 0;
  font-size: 14px;
  color: #888;
  text-align: center;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  position: relative;
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

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  cursor: pointer;
  user-select: none;
}

.remember-me input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: #667eea;
  cursor: pointer;
}

.forgot-link {
  color: #667eea;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.login-btn {
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

.login-btn:hover {
  opacity: 0.9;
}

.login-btn:active {
  transform: scale(0.98);
}

.error-msg {
  margin: 0;
  padding: 10px 14px;
  font-size: 14px;
  color: #e74c3c;
  background: #fef0ef;
  border: 1px solid #fadbd8;
  border-radius: 6px;
  text-align: center;
}

.register-link {
  margin: 24px 0 0 0;
  font-size: 14px;
  color: #888;
  text-align: center;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
