<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get, put, remove } from '@/net'

interface Notice { id:number; actorNickname:string; type:string; postId:number|null; commentId:number|null; content:string; createdAt:string; read:boolean; postDeleted:boolean; commentDeleted:boolean }
const router = useRouter()
const notices = ref<Notice[]>([])
const filter = ref('all')
const page = reactive({ current: 1, size: 20, total: 0, unread: 0 })
const loading = ref(false)
function load() { loading.value = true; get('/api/notification', { page: page.current, size: page.size, filter: filter.value }, r => { const d=r.data?.data; notices.value=d?.items??[]; page.total=d?.total??0; page.unread=d?.unreadCount??0; loading.value=false }, ()=>loading.value=false, ()=>loading.value=false) }
function switchFilter(v:string) { filter.value=v; page.current=1; load() }
function markRead(n:Notice, go=true) { if (!n.read) put(`/api/notification/${n.id}/read`, undefined, () => { n.read=true; page.unread=Math.max(0,page.unread-1) }); if (go) { if (n.postId && !n.postDeleted) router.push({ path:`/post/${n.postId}`, query:n.commentId&&!n.commentDeleted?{commentId:String(n.commentId)}:undefined }); else if (n.postId) router.push(`/post/${n.postId}`) } }
function markAll() { put('/api/notification/read-all', undefined, () => { notices.value.forEach(n=>n.read=true); page.unread=0 }) }
function deleteOne(n:Notice) { remove(`/api/notification/${n.id}`, () => { notices.value=notices.value.filter(x=>x.id!==n.id); page.total=Math.max(0,page.total-1); if(!n.read) page.unread=Math.max(0,page.unread-1) }) }
function clearRead() { remove('/api/notification/read', () => { notices.value=notices.value.filter(n=>!n.read); load() }) }
onMounted(load)
</script>

<template>
  <div class="page"><header><strong @click="router.push('/index')">资源分享社区</strong><button @click="router.push('/index')">返回帖子流</button></header>
    <main><div class="title-row"><h1>消息</h1><div class="actions"><button :class="{active:filter==='all'}" @click="switchFilter('all')">全部</button><button :class="{active:filter==='unread'}" @click="switchFilter('unread')">未读<span v-if="page.unread">（{{page.unread}}）</span></button><button @click="markAll">全部已读</button><button @click="clearRead">清空已读</button></div></div>
      <p v-if="!loading&&!notices.length" class="empty">暂无通知</p>
      <article v-for="n in notices" :key="n.id" :class="{unread:!n.read}" @click="markRead(n)"><div><strong>{{n.actorNickname}}</strong> {{n.content}}</div><small>{{n.createdAt}}</small><p v-if="n.postDeleted" class="deleted">相关帖子已删除</p><p v-else-if="n.commentDeleted" class="deleted">相关评论已删除</p><button class="delete" @click.stop="deleteOne(n)">删除</button></article>
      <div v-if="page.total>page.size" class="pager"><button :disabled="page.current<=1" @click="page.current--;load()">上一页</button><span>第 {{page.current}} / {{Math.ceil(page.total/page.size)}} 页</span><button :disabled="page.current>=Math.ceil(page.total/page.size)" @click="page.current++;load()">下一页</button></div>
    </main>
  </div>
</template>
<style scoped>
.page{min-height:100vh;background:#f5f6fa;color:#242638}header{height:60px;background:#fff;border-bottom:1px solid #e8e8e8;display:flex;align-items:center;justify-content:space-between;padding:0 calc((100% - 860px)/2 + 20px)}header strong{color:#667eea;cursor:pointer}button{padding:7px 14px;border:1px solid #d9dcf5;background:#fff;color:#667eea;border-radius:6px;cursor:pointer}.active{background:#667eea;color:#fff}main{max-width:860px;margin:auto;padding:24px 20px}.title-row{display:flex;justify-content:space-between;align-items:center;gap:12px}.actions{display:flex;gap:8px;flex-wrap:wrap}article{position:relative;background:#fff;padding:18px 24px;margin:12px 0;border-radius:8px;cursor:pointer;border-left:3px solid transparent}article.unread{border-left-color:#667eea;background:#fbfbff}article small{display:block;color:#aaa;margin-top:8px}.deleted{color:#999}.delete{position:absolute;right:18px;bottom:14px;color:#e74c3c;border-color:#f5cccc}.empty{text-align:center;color:#999;padding:60px}.pager{display:flex;justify-content:center;gap:16px;align-items:center;margin-top:20px}
</style>
