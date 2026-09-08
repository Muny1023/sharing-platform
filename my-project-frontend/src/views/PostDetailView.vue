<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get, post as sendPost, remove } from '@/net'

interface PostDetail {
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
  updateTime: string
}

interface CommentNode {
  id: number
  postId: number
  authorId: number
  authorNickname: string
  parentId: number | null
  content: string
  likeCount: number
  createTime: string
  replies: CommentNode[]
}

const route = useRoute()
const router = useRouter()
const postId = computed(() => Number(route.params.id))

const post = ref<PostDetail | null>(null)
const comments = ref<CommentNode[]>([])
const postLiked = ref(false)
const likedCommentIds = ref<Set<number>>(new Set())
const commentText = ref('')
/** 回复框状态：正在回复的顶级评论 id + 预填文本 */
const replyState = reactive({ activeParentId: null as number | null, text: '' })
const submitting = ref(false)
const notFound = ref(false)
const tombstone = ref(false)
const meId = ref<number | null>(null)
const favorited = ref(false)

/** 展开楼层里所有评论 id（含回复），用于批量查点赞状态 */
function collectCommentIds(nodes: CommentNode[]): number[] {
  return nodes.flatMap(node => [node.id, ...node.replies.map(reply => reply.id)])
}

function loadPost() {
  get(`/api/post/${postId.value}`, {},
    (res) => { post.value = res.data?.data ?? null },
    (res) => { if (res.data?.code === 410) tombstone.value = true; else notFound.value = true },
  )
}
function loadFavorite() { get('/api/favorite/status', { postId: postId.value }, r => { favorited.value = !!r.data?.data }) }
function toggleFavorite() { sendPost('/api/favorite/toggle', { postId: postId.value }, r => { const next = !!r.data?.data?.favorited; if (post.value && next !== favorited.value) post.value.favoriteCount = Math.max(0, (post.value.favoriteCount ?? 0) + (next ? 1 : -1)); favorited.value = next }) }
function deletePost() { if (!confirm('确定删除这篇帖子吗？')) return; remove(`/api/post/${postId.value}`, () => router.push('/index')) }

function loadPostLikeStatus() {
  get('/api/like/status', { targetType: 'post', targetIds: String(postId.value) },
    (res) => { postLiked.value = (res.data?.data ?? []).includes(postId.value) },
  )
}

function loadComments() {
  get('/api/comment/list/' + postId.value, {},
    (res) => {
      comments.value = res.data?.data ?? []
      const ids = collectCommentIds(comments.value)
      if (ids.length === 0) return
      get('/api/like/status', { targetType: 'comment', targetIds: ids.join(',') },
        (likeRes) => { likedCommentIds.value = new Set(likeRes.data?.data ?? []) },
      )
    },
  )
}

function togglePostLike() {
  sendPost('/api/like/toggle', { targetType: 'post', targetId: postId.value },
    (res) => {
      postLiked.value = !!res.data?.data?.liked
      if (post.value) post.value.likeCount += postLiked.value ? 1 : -1
    },
  )
}

function toggleCommentLike(comment: CommentNode) {
  sendPost('/api/like/toggle', { targetType: 'comment', targetId: comment.id },
    (res) => {
      const liked = !!res.data?.data?.liked
      if (liked) likedCommentIds.value.add(comment.id)
      else likedCommentIds.value.delete(comment.id)
      comment.likeCount += liked ? 1 : -1
    },
  )
}

function submitComment() {
  const text = commentText.value.trim()
  if (!text) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  sendPost('/api/comment', { postId: postId.value, parentId: null, content: text },
    () => {
      submitting.value = false
      commentText.value = ''
      if (post.value) post.value.commentCount += 1
      loadComments()
    },
    () => { submitting.value = false },
    () => { submitting.value = false },
  )
}

function startReply(parentId: number, nickname: string) {
  replyState.activeParentId = parentId
  replyState.text = `@${nickname} `
}

function cancelReply() {
  replyState.activeParentId = null
  replyState.text = ''
}

function submitReply(parentId: number) {
  const text = replyState.text.trim()
  if (!text) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  submitting.value = true
  sendPost('/api/comment', { postId: postId.value, parentId, content: text },
    () => {
      submitting.value = false
      cancelReply()
      if (post.value) post.value.commentCount += 1
      loadComments()
    },
    () => { submitting.value = false },
    () => { submitting.value = false },
  )
}

onMounted(() => {
  loadPost()
  loadPostLikeStatus()
  loadFavorite()
  loadComments()
  get('/api/user/me', {}, res => { meId.value = res.data?.data?.id ?? null })
})
</script>

<template>
  <div class="detail-wrapper">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="router.push('/index')">资源分享社区</div>
        <button class="back-btn" @click="router.push('/index')">返回帖子流</button>
      </div>
    </header>

    <main class="detail-main">
      <div v-if="notFound" class="empty-tip">帖子不存在</div>
      <div v-else-if="tombstone" class="tombstone"><h1>该帖子已被作者删除</h1><p>原帖内容已不可用</p><button class="back-btn" @click="router.push('/index')">返回帖子流</button></div>

      <template v-else-if="post">
        <article class="post-panel">
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <span class="meta-author">{{ post.authorNickname }}</span>
            <span class="meta-time">发布于 {{ post.createTime }}</span>
          </div>
          <div class="post-content">{{ post.content }}</div>
          <a :href="post.resourceUrl" target="_blank" rel="noopener noreferrer" class="resource-link">
            🔗 获取资源：{{ post.resourceUrl }}
          </a>
          <div class="post-actions">
            <button class="like-btn" :class="{ liked: postLiked }" @click="togglePostLike">
              {{ postLiked ? '已赞' : '点赞' }} · {{ post.likeCount }}
            </button>
            <span class="comment-total">共 {{ post.commentCount }} 条评论</span>
            <button class="favorite-btn" :class="{liked:favorited}" @click="toggleFavorite">{{ favorited ? '已收藏' : '收藏' }} · {{ post.favoriteCount ?? 0 }}</button>
            <template v-if="meId === post.authorId"><button class="edit-btn" @click="router.push(`/post/${post.id}/edit`)">编辑</button><button class="delete-btn" @click="deletePost">删除</button></template>
          </div>
        </article>

        <section class="comment-panel">
          <h2 class="panel-title">评论区</h2>

          <div class="comment-composer">
            <textarea
              v-model="commentText"
              class="composer-input"
              rows="3"
              maxlength="1000"
              placeholder="写下你的评论..."
            ></textarea>
            <div class="composer-foot">
              <span class="composer-count">{{ commentText.length }} / 1000</span>
              <button class="submit-btn" :disabled="submitting" @click="submitComment">
                {{ submitting ? '发布中...' : '发布评论' }}
              </button>
            </div>
          </div>

          <p v-if="comments.length === 0" class="empty-tip small">还没有评论，来抢沙发吧</p>

          <div v-for="comment in comments" :key="comment.id" class="comment-thread">
            <div class="comment-item">
              <div class="comment-head">
                <span class="comment-author">{{ comment.authorNickname }}</span>
                <span class="comment-time">{{ comment.createTime }}</span>
              </div>
              <p class="comment-body">{{ comment.content }}</p>
              <div class="comment-ops">
                <button
                  class="op-btn"
                  :class="{ liked: likedCommentIds.has(comment.id) }"
                  @click="toggleCommentLike(comment)"
                >👍 {{ comment.likeCount }}</button>
                <button class="op-btn" @click="replyState.activeParentId === comment.id ? cancelReply() : startReply(comment.id, comment.authorNickname)">回复</button>
              </div>

              <div v-if="replyState.activeParentId === comment.id" class="reply-composer">
                <textarea
                  v-model="replyState.text"
                  class="composer-input"
                  rows="2"
                  maxlength="1000"
                  placeholder="回复..."
                ></textarea>
                <div class="composer-foot">
                  <button class="cancel-btn" @click="cancelReply">取消</button>
                  <button class="submit-btn" :disabled="submitting" @click="submitReply(comment.id)">
                    {{ submitting ? '发布中...' : '发布回复' }}
                  </button>
                </div>
              </div>
            </div>

            <div v-for="reply in comment.replies" :key="reply.id" class="comment-item reply">
              <div class="comment-head">
                <span class="comment-author">{{ reply.authorNickname }}</span>
                <span class="comment-time">{{ reply.createTime }}</span>
              </div>
              <p class="comment-body">{{ reply.content }}</p>
              <div class="comment-ops">
                <button
                  class="op-btn"
                  :class="{ liked: likedCommentIds.has(reply.id) }"
                  @click="toggleCommentLike(reply)"
                >👍 {{ reply.likeCount }}</button>
              </div>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.detail-wrapper {
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

.detail-main {
  flex: 1;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 20px 48px;
  box-sizing: border-box;
}

.post-panel {
  padding: 28px 32px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.post-title {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.post-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  font-size: 13px;
  color: #999;
}

.meta-author {
  color: #667eea;
  font-weight: 500;
}

.post-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.resource-link {
  display: inline-block;
  margin-top: 20px;
  padding: 10px 16px;
  font-size: 14px;
  color: #667eea;
  background: #f0f2fe;
  border: 1px solid #d9dcf5;
  border-radius: 6px;
  text-decoration: none;
  word-break: break-all;
}

.resource-link:hover {
  background: #e4e8fd;
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
}

.like-btn {
  padding: 8px 22px;
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
  background: #fff;
  border: 1px solid #d9dcf5;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.like-btn.liked {
  color: #fff;
  background: #667eea;
  border-color: #667eea;
}

.comment-total {
  font-size: 13px;
  color: #999;
}
.favorite-btn,.edit-btn,.delete-btn { padding: 7px 14px; border:1px solid #d9dcf5; border-radius:6px; background:#fff; color:#667eea; cursor:pointer; }
.favorite-btn.liked { color:#fff; background:#f39c12; border-color:#f39c12; }
.delete-btn { color:#e74c3c; border-color:#f5cccc; }
.tombstone { padding:80px 20px; text-align:center; background:#fff; border-radius:8px; color:#888; }
.tombstone h1 { color:#555; }

.comment-panel {
  padding: 24px 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.panel-title {
  margin: 0 0 18px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.comment-composer,
.reply-composer {
  margin-bottom: 24px;
}

.composer-input {
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  background: #f5f6fa;
  border: 1.5px solid #e8e8e8;
  border-radius: 8px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.composer-input:focus {
  border-color: #667eea;
  background: #fff;
}

.composer-foot {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.composer-count {
  margin-right: auto;
  font-size: 12px;
  color: #bbb;
}

.submit-btn {
  padding: 8px 22px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: #666;
  background: none;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
}

.comment-thread {
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid #f5f5f5;
}

.comment-item {
  padding: 12px 0;
}

.comment-item.reply {
  margin-left: 40px;
  padding: 10px 0 4px 0;
  border-left: 2px solid #f0f2fe;
  padding-left: 16px;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
}

.comment-time {
  font-size: 12px;
  color: #bbb;
}

.comment-body {
  margin: 0 0 8px 0;
  font-size: 14px;
  line-height: 1.7;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-ops {
  display: flex;
  gap: 14px;
}

.op-btn {
  padding: 4px 10px;
  font-size: 13px;
  color: #999;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.op-btn:hover {
  color: #667eea;
  background: #f0f2fe;
}

.op-btn.liked {
  color: #667eea;
  font-weight: 600;
}

.empty-tip {
  padding: 60px 0;
  font-size: 14px;
  color: #999;
  text-align: center;
  background: #fff;
  border-radius: 8px;
}

.empty-tip.small {
  padding: 30px 0;
}
</style>
