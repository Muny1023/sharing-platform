package org.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Account;
import org.example.entity.dto.Comment;
import org.example.entity.dto.Notification;
import org.example.entity.dto.Post;
import org.example.entity.vo.response.NotificationPageVO;
import org.example.entity.vo.response.NotificationVO;
import org.example.mapper.NotificationMapper;
import org.example.service.AccountService;
import org.example.service.CommentService;
import org.example.service.NotificationService;
import org.example.service.PostService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    private static final String REDIS_PREFIX = "notification:unread:";

    @Resource StringRedisTemplate redis;
    @Resource AccountService accountService;
    @Resource PostService postService;
    @Resource CommentService commentService;

    @Override
    public long unreadCount(Integer recipientId) {
        String key = REDIS_PREFIX + recipientId;
        String cached = redis.opsForValue().get(key);
        if (cached != null) return parse(cached);
        long count = count(Wrappers.<Notification>query().eq("recipient_id", recipientId).eq("is_read", false));
        redis.opsForValue().set(key, String.valueOf(count));
        return count;
    }

    @Override
    public NotificationPageVO page(Integer recipientId, int page, int size, String filter) {
        Page<Notification> result = new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 100));
        var query = Wrappers.<Notification>query().eq("recipient_id", recipientId).orderByDesc("created_at");
        if ("unread".equalsIgnoreCase(filter)) query.eq("is_read", false);
        else if ("read".equalsIgnoreCase(filter)) query.eq("is_read", true);
        page(result, query);
        Map<Integer, String> names = result.getRecords().isEmpty() ? Map.of() : accountService.listByIds(result.getRecords().stream().map(Notification::getActorId).distinct().toList()).stream().collect(Collectors.toMap(Account::getId, Account::getNickname, (a,b)->a));
        var items = result.getRecords().stream().map(n -> {
            Post post = n.getPostId() == null ? null : postService.getById(n.getPostId());
            Comment comment = n.getCommentId() == null ? null : commentService.getById(n.getCommentId());
            return new NotificationVO(n.getId(), n.getActorId(), names.getOrDefault(n.getActorId(), "未知用户"), n.getType(), n.getPostId(), n.getCommentId(), n.getContent(), n.getCreatedAt(), Boolean.TRUE.equals(n.getIsRead()), post == null && n.getPostId() != null, comment == null && n.getCommentId() != null);
        }).toList();
        return new NotificationPageVO(result.getCurrent(), result.getSize(), result.getTotal(), unreadCount(recipientId), items);
    }

    @Override @Transactional
    public String markRead(Integer recipientId, Integer notificationId) {
        Notification n = getOne(Wrappers.<Notification>query().eq("id", notificationId).eq("recipient_id", recipientId));
        if (n == null) return "通知不存在";
        if (!Boolean.TRUE.equals(n.getIsRead())) { update().eq("id", notificationId).set("is_read", true).update(); decrement(recipientId, 1); }
        return null;
    }

    @Override @Transactional
    public long markAllRead(Integer recipientId) {
        long count = count(Wrappers.<Notification>query().eq("recipient_id", recipientId).eq("is_read", false));
        if (count > 0) update().eq("recipient_id", recipientId).eq("is_read", false).set("is_read", true).update();
        redis.opsForValue().set(REDIS_PREFIX + recipientId, "0");
        return count;
    }

    @Override @Transactional
    public String deleteOne(Integer recipientId, Integer notificationId) {
        Notification n = getOne(Wrappers.<Notification>query().eq("id", notificationId).eq("recipient_id", recipientId));
        if (n == null) return "通知不存在";
        removeById(notificationId);
        if (!Boolean.TRUE.equals(n.getIsRead())) decrement(recipientId, 1);
        return null;
    }

    @Override @Transactional
    public long deleteRead(Integer recipientId) {
        long count = count(Wrappers.<Notification>query().eq("recipient_id", recipientId).eq("is_read", true));
        if (count > 0) remove(Wrappers.<Notification>query().eq("recipient_id", recipientId).eq("is_read", true));
        return count;
    }

    @Override @Transactional
    public void consume(NotificationEvent event) {
        if (event == null || event.recipientId() == null || event.actorId() == null || event.recipientId().equals(event.actorId())) return;
        if (getOne(Wrappers.<Notification>query().eq("event_id", event.eventId())) != null) return;
        Notification n = new Notification(null, event.recipientId(), event.actorId(), event.type(), event.postId(), event.commentId(), event.content(), new Date(), false, event.eventId());
        save(n);
        redis.opsForValue().increment(REDIS_PREFIX + event.recipientId());
    }

    @Override @Scheduled(cron = "0 0 3 * * *")
    public long cleanupExpired() {
        Date cutoff = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90));
        return baseMapper.delete(Wrappers.<Notification>query().lt("created_at", cutoff));
    }

    @Override
    public List<Integer> listUnreadCommentIds(Integer recipientId, Integer postId) {
        return list(Wrappers.<Notification>query().eq("recipient_id", recipientId).eq("post_id", postId)
                .eq("is_read", false).in("type", List.of("POST_COMMENT", "COMMENT_REPLY")))
                .stream().map(Notification::getCommentId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private void decrement(Integer id, long delta) {
        redis.opsForValue().decrement(REDIS_PREFIX + id, delta);
        String value = redis.opsForValue().get(REDIS_PREFIX + id);
        if (value != null && parse(value) < 0) redis.opsForValue().set(REDIS_PREFIX + id, "0");
    }
    private long parse(String value) { try { return Math.max(0, Long.parseLong(value)); } catch (NumberFormatException e) { return 0; } }
}
