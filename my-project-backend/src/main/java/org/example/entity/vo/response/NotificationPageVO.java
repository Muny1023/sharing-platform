package org.example.entity.vo.response;

import java.util.List;

public record NotificationPageVO(long page, long size, long total, long unreadCount, List<NotificationVO> items) {
}
