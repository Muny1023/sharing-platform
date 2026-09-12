<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { aiChat } from '@/net'

interface PostCard {
  postId: number
  title: string
  snippet: string
  path: string
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  posts?: PostCard[]
}

const router = useRouter()
const message = ref('')
const messages = ref<Message[]>([])
const loading = ref(false)
const conversationId = ref(sessionStorage.getItem('ai_conversation_id') || '')

function send() {
  const text = message.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  message.value = ''
  loading.value = true
  aiChat(
    { conversationId: conversationId.value || undefined, message: text },
    (res) => {
      const data = res.data?.data
      if (data?.conversationId) {
        conversationId.value = data.conversationId
        sessionStorage.setItem('ai_conversation_id', data.conversationId)
      }
      messages.value.push({ role: 'assistant', content: data?.reply || '暂时没有得到有效回答', posts: data?.posts || [] })
      loading.value = false
      nextTick(() => document.querySelector('.messages')?.scrollTo({ top: 999999, behavior: 'smooth' }))
    },
    () => { loading.value = false },
    () => {
      messages.value.push({ role: 'assistant', content: 'AI 服务暂时没有响应，请稍后重试。' })
      loading.value = false
    },
  )
}

function clearConversation() {
  conversationId.value = ''
  sessionStorage.removeItem('ai_conversation_id')
  messages.value = []
}
</script>

<template>
  <div class="ai-page">
    <header class="topbar">
      <div class="topbar-inner">
        <button class="back-btn" @click="router.push('/index')">返回帖子流</button>
        <strong>AI 聊天</strong>
        <button class="clear-btn" @click="clearConversation">清空对话</button>
      </div>
    </header>
    <main class="chat-shell">
      <div class="messages">
        <div v-if="messages.length === 0" class="welcome">
          告诉我你想找什么资源，例如“帮我找爵士音乐”。
        </div>
        <div v-for="(item, index) in messages" :key="index" class="message-row" :class="item.role">
          <div class="bubble">{{ item.content }}</div>
          <div v-if="item.posts?.length" class="post-cards">
            <button v-for="post in item.posts" :key="post.postId" class="post-card" @click="router.push(post.path)">
              <strong>{{ post.title }}</strong>
              <span>{{ post.snippet }}</span>
              <small>查看帖子 →</small>
            </button>
          </div>
        </div>
        <div v-if="loading" class="loading">正在搜索...</div>
      </div>
      <form class="composer" @submit.prevent="send">
        <input v-model="message" maxlength="500" placeholder="描述你想找的资源" :disabled="loading">
        <button type="submit" :disabled="loading || !message.trim()">发送</button>
      </form>
    </main>
  </div>
</template>

<style scoped>
.ai-page { min-height:100vh; background:#f5f6fa; color:#20212b; }
.topbar { background:#fff; border-bottom:1px solid #e8e8e8; }
.topbar-inner { max-width:860px; height:60px; margin:0 auto; padding:0 20px; display:flex; align-items:center; justify-content:space-between; }
.topbar strong { color:#667eea; }
.back-btn,.clear-btn { border:0; background:none; color:#667eea; cursor:pointer; padding:8px; }
.chat-shell { max-width:860px; min-height:calc(100vh - 60px); margin:0 auto; padding:24px 20px; display:flex; flex-direction:column; box-sizing:border-box; }
.messages { flex:1; overflow:auto; padding:8px 0 20px; }
.welcome,.loading { color:#888; text-align:center; padding:40px 0; }
.message-row { display:flex; flex-direction:column; margin:0 0 18px; max-width:78%; }
.message-row.user { margin-left:auto; align-items:flex-end; }
.bubble { white-space:pre-wrap; line-height:1.7; padding:11px 14px; border-radius:10px; background:#fff; box-shadow:0 1px 3px rgba(0,0,0,.05); }
.user .bubble { background:#667eea; color:#fff; }
.post-cards { width:100%; display:flex; flex-direction:column; gap:8px; margin-top:8px; }
.post-card { border:1px solid #e2e4f2; background:#fff; border-radius:8px; padding:12px; text-align:left; cursor:pointer; display:flex; flex-direction:column; gap:6px; }
.post-card:hover { border-color:#667eea; }
.post-card strong { color:#333; }
.post-card span { color:#666; line-height:1.5; }
.post-card small { color:#667eea; }
.composer { display:flex; gap:10px; padding-top:14px; border-top:1px solid #e3e4ea; }
.composer input { flex:1; min-width:0; border:1px solid #dfe1ea; border-radius:8px; padding:12px; font-size:15px; }
.composer button { width:76px; border:0; border-radius:8px; background:#667eea; color:#fff; cursor:pointer; }
.composer button:disabled { opacity:.5; cursor:not-allowed; }
@media (max-width:600px) { .message-row { max-width:92%; } .topbar-inner { padding:0 12px; } .chat-shell { padding:16px 12px; } }
</style>
