package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Notification;
import org.example.entity.vo.response.NotificationPageVO;

public interface NotificationService extends IService<Notification> {
    long unreadCount(Integer recipientId);
    NotificationPageVO page(Integer recipientId, int page, int size, String filter);
    String markRead(Integer recipientId, Integer notificationId);
    long markAllRead(Integer recipientId);
    String deleteOne(Integer recipientId, Integer notificationId);
    long deleteRead(Integer recipientId);
    void consume(NotificationEvent event);
    long cleanupExpired();

    record NotificationEvent(String eventId, Integer actorId, Integer recipientId, String type,
                             Integer postId, Integer commentId, String content) {}
}
