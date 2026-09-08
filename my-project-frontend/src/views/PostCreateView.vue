<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { post } from '@/net'

const router = useRouter()

const form = reactive({
  title: '',
  content: '',
  resourceUrl: '',
})
const submitting = ref(false)

const URL_PATTERN = /^https?:\/\/\S+$/

function validate(): string | null {
  if (!form.title.trim()) return '标题不能为空'
  if (form.title.length > 100) return '标题不能超过100字'
  if (!form.content.trim()) return '正文不能为空'
  if (form.content.length > 10000) return '正文不能超过10000字'
  if (!form.resourceUrl.trim()) return '资源链接不能为空'
  if (!URL_PATTERN.test(form.resourceUrl.trim())) return '资源链接必须以 http(s):// 开头'
  return null
}

function handleSubmit() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  submitting.value = true
  post('/api/post',
    { title: form.title.trim(), content: form.content, resourceUrl: form.resourceUrl.trim() },
    () => {
      submitting.value = false
      ElMessage.success('发布成功')
      router.push('/index')
    },
    (res) => {
      submitting.value = false
      ElMessage.error(res.data?.message || '发布失败')
    },
    () => { submitting.value = false },
  )
}
</script>

<template>
  <div class="create-wrapper">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="router.push('/index')">资源分享社区</div>
        <button class="back-btn" @click="router.push('/index')">返回帖子流</button>
      </div>
    </header>

    <main class="create-main">
      <div class="create-card">
        <h1 class="create-title">发布资源</h1>
        <p class="create-desc">分享一个资源：写清标题和说明，附上可访问的资源链接</p>

        <form class="create-form" @submit.prevent>
          <div class="form-group">
            <label class="form-label">标题 <span class="label-count">{{ form.title.length }} / 100</span></label>
            <input
              v-model="form.title"
              type="text"
              class="form-input"
              maxlength="100"
              placeholder="一句话说清这是什么资源"
            />
          </div>

          <div class="form-group">
            <label class="form-label">资源链接 <span class="label-count">必须以 http(s):// 开头</span></label>
            <input
              v-model="form.resourceUrl"
              type="text"
              class="form-input"
              maxlength="2048"
              placeholder="https://example.com/resource"
            />
          </div>

          <div class="form-group">
            <label class="form-label">正文 <span class="label-count">{{ form.content.length }} / 10000</span></label>
            <textarea
              v-model="form.content"
              class="form-textarea"
              rows="10"
              maxlength="10000"
              placeholder="介绍一下这个资源的内容、适用场景、使用方法..."
            ></textarea>
          </div>

          <button type="submit" class="submit-btn" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '发布中...' : '立即发布' }}
          </button>
        </form>
      </div>
    </main>
  </div>
</template>

<style scoped>
.create-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f5f6fa;
}

.topbar {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.topbar-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 860px;
  height: 60px;
  margin: 0 auto;
  padding: 0 20px;
}

.brand {
  font-size: 18px;
  font-weight: 700;
  color: #667eea;
  cursor: pointer;
}

.back-btn {
  padding: 8px 14px;
  font-size: 14px;
  color: #667eea;
  background: none;
  border: 1px solid #d9dcf5;
  border-radius: 6px;
  cursor: pointer;
}

.create-main {
  flex: 1;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 20px 48px;
  box-sizing: border-box;
}

.create-card {
  padding: 32px 36px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.create-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.create-desc {
  margin: 0 0 28px 0;
  font-size: 14px;
  color: #888;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.label-count {
  font-size: 12px;
  font-weight: 400;
  color: #bbb;
}

.form-input {
  padding: 12px 14px;
  font-size: 15px;
  color: #333;
  background: #f5f6fa;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.25s, box-shadow 0.25s, background 0.25s;
}

.form-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #fff;
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #b0b0b0;
}

.form-textarea {
  padding: 14px;
  font-size: 15px;
  line-height: 1.7;
  color: #333;
  background: #f5f6fa;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.25s, box-shadow 0.25s, background 0.25s;
}

.form-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #fff;
}

.submit-btn {
  align-self: flex-start;
  padding: 12px 40px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.25s, transform 0.15s;
}

.submit-btn:hover {
  opacity: 0.9;
}

.submit-btn:active {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
