package org.example.service;

import org.example.entity.dto.Notification;
import org.example.mapper.NotificationMapper;
import org.example.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationMapper notificationMapper;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @Spy @InjectMocks NotificationServiceImpl service;

    @Test
    void consume_selfAction_isIgnored() {
        service.consume(new NotificationService.NotificationEvent("e1", 1, 1, "POST_LIKE", 7, null, "有人赞了你的帖子"));
        verify(service, never()).save(any(Notification.class));
        verifyNoInteractions(redis);
    }

    @Test
    void consume_duplicateEvent_isIgnored() {
        doReturn(new Notification(1, 2, 1, "POST_LIKE", 7, null, "有人赞了你的帖子", null, false, "e1"))
                .when(service).getOne(any());
        service.consume(new NotificationService.NotificationEvent("e1", 1, 2, "POST_LIKE", 7, null, "有人赞了你的帖子"));
        verify(service, never()).save(any(Notification.class));
    }

    @Test
    void consume_newEvent_savesAndIncrementsUnread() {
        doReturn(null).when(service).getOne(any());
        doReturn(true).when(service).save(any(Notification.class));
        when(redis.opsForValue()).thenReturn(values);
        service.consume(new NotificationService.NotificationEvent("e2", 1, 2, "POST_COMMENT", 7, 9, "评论了你的帖子"));
        verify(service).save(argThat(n -> n.getRecipientId() == 2 && n.getActorId() == 1 && "e2".equals(n.getEventId())));
        verify(values).increment("notification:unread:2");
    }
}
