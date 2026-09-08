<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get, logout } from '@/net'

interface PostItem {
  id: number
  authorId: number
  authorNickname: string
  title: string
  content: string
  resourceUrl: string
  likeCount: number
  commentCount: number
  favoriteCount: number
  createTime: string
  postId?: number
  postTitle?: string
}

interface AccountInfo {
  id: number
  username: string
  nickname: string
  avatar: string | null
  role: string
}

const router = useRouter()

const me = ref<AccountInfo | null>(null)
const posts = ref<PostItem[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })
const sort = ref<'latest' | 'hot' | 'oldest'>('latest')
const loading = ref(false)
const keyword = ref('')
const searchMode = ref<'posts'|'comments'>('posts')
const searching = ref(false)

function loadPosts() {
  loading.value = true
  get('/api/post/list',
    { page: page.current, size: page.size, sort: sort.value },
    (res) => {
      const data = res.data?.data
      posts.value = data?.items ?? []
      page.total = data?.total ?? 0
      loading.value = false
    },
    () => { loading.value = false },
    () => { loading.value = false },
  )
}

function search() {
  if (!keyword.value.trim()) { loadPosts(); return }
  searching.value = true
  get(searchMode.value === 'posts' ? '/api/post/search' : '/api/post/search-comments', { keyword: keyword.value.trim(), page: page.current, size: page.size }, (res) => {
    const data = res.data?.data; posts.value = data?.items ?? []; page.total = data?.total ?? 0; searching.value = false
  }, () => { searching.value = false }, () => { searching.value = false })
}

function switchSort(value: 'latest' | 'hot' | 'oldest') {
  if (sort.value === value) return
  sort.value = value
  page.current = 1
  loadPosts()
}

function handlePageChange(newPage: number) {
  page.current = newPage
  loadPosts()
}

/** 列表卡片只展示正文摘要，避免长文撑爆卡片 */
function excerpt(content: string): string {
  return content.length > 120 ? content.slice(0, 120) + '…' : content
}

function handleLogout() {
  logout(() => {
    router.push('/')
  })
}

onMounted(() => {
  loadPosts()
  get('/api/user/me', {},
    (res) => { me.value = res.data?.data ?? null },
  )
})
</script>

<template>
  <div class="index-wrapper">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="router.push('/index')">资源分享社区</div>
        <div class="topbar-right">
          <span class="welcome user-menu" @click="router.push('/my/posts')">你好，{{ me?.nickname || me?.username || '...' }}</span>
          <button class="link-btn" @click="router.push('/my/favorites')">我的收藏</button>
          <button class="new-post-btn" @click="router.push('/post/new')">发布资源</button>
          <button class="logout-btn" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="feed">
      <div class="feed-inner">
        <div class="sort-bar">
          <button
            v-for="option in [{ value: 'latest', label: '最新' }, { value: 'hot', label: '最热' }, { value: 'oldest', label: '最早' }]"
            :key="option.value"
            class="sort-btn"
            :class="{ active: sort === option.value }"
            @click="switchSort(option.value as 'latest' | 'hot' | 'oldest')"
          >{{ option.label }}</button>
        </div>
        <div class="search-bar">
          <select v-model="searchMode"><option value="posts">帖子搜索</option><option value="comments">评论搜索</option></select>
          <input v-model="keyword" placeholder="输入关键词" @keyup.enter="search">
          <button @click="search">搜索</button>
          <button v-if="keyword" class="clear-btn" @click="keyword='';loadPosts()">清除</button>
        </div>

        <p v-if="!loading && posts.length === 0" class="empty-tip">
          还没有任何帖子，点击右上角「发布资源」分享第一个资源吧
        </p>

        <article
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="router.push(`/post/${post.postId ?? post.id}`)"
        >
          <h2 class="post-title">{{ post.postTitle ?? post.title }}</h2>
          <p class="post-excerpt">{{ excerpt(post.content) }}</p>
          <div class="post-meta">
            <span class="meta-author">{{ post.authorNickname }}</span>
            <span class="meta-time">{{ post.createTime }}</span>
            <span class="meta-count">👍 {{ post.likeCount }}</span>
            <span class="meta-count">💬 {{ post.commentCount }}</span>
            <span class="meta-count">★ {{ post.favoriteCount ?? 0 }}</span>
          </div>
        </article>

        <div v-if="page.total > page.size" class="pager">
          <button :disabled="page.current <= 1 || loading" @click="handlePageChange(page.current - 1)">上一页</button>
          <span class="pager-info">第 {{ page.current }} / {{ Math.ceil(page.total / page.size) }} 页 · 共 {{ page.total }} 帖</span>
          <button :disabled="page.current >= Math.ceil(page.total / page.size) || loading" @click="handlePageChange(page.current + 1)">下一页</button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.index-wrapper {
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

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.welcome {
  font-size: 14px;
  color: #666;
}
.user-menu { cursor: pointer; color: #667eea; }
.link-btn { border: 0; background: none; color: #667eea; cursor: pointer; }
.search-bar { display:flex; gap:8px; margin: 0 0 18px; }
.search-bar select, .search-bar input { padding: 8px 10px; border:1px solid #e4e5ee; border-radius:6px; background:#fff; }
.search-bar input { flex:1; min-width:0; }
.search-bar button { padding:8px 14px; border:1px solid #d9dcf5; border-radius:6px; background:#fff; color:#667eea; cursor:pointer; }
.search-bar button:first-of-type { color:#fff; background:#667eea; }

.new-post-btn {
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.new-post-btn:hover {
  opacity: 0.9;
}

.logout-btn {
  padding: 8px 14px;
  font-size: 14px;
  color: #e74c3c;
  background: none;
  border: 1px solid #fadbd8;
  border-radius: 6px;
  cursor: pointer;
}

.logout-btn:hover {
  background: #fef0ef;
}

.feed {
  flex: 1;
  padding: 24px 20px 48px;
}

.feed-inner {
  max-width: 860px;
  margin: 0 auto;
}

.sort-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.sort-btn {
  padding: 6px 16px;
  font-size: 14px;
  color: #666;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.sort-btn.active {
  color: #fff;
  background: #667eea;
  border-color: #667eea;
}

.empty-tip {
  padding: 60px 0;
  font-size: 14px;
  color: #999;
  text-align: center;
  background: #fff;
  border-radius: 8px;
}

.post-card {
  padding: 20px 24px;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.post-card:hover {
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.18);
  transform: translateY(-2px);
}

.post-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.post-excerpt {
  margin: 0 0 14px 0;
  font-size: 14px;
  line-height: 1.7;
  color: #666;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.meta-author {
  color: #667eea;
  font-weight: 500;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.pager button {
  padding: 8px 18px;
  font-size: 14px;
  color: #667eea;
  background: #fff;
  border: 1px solid #d9dcf5;
  border-radius: 6px;
  cursor: pointer;
}

.pager button:disabled {
  color: #ccc;
  border-color: #eee;
  cursor: not-allowed;
}

.pager-info {
  font-size: 13px;
  color: #999;
}
</style>
